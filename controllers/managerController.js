const Task = require('../models/Task');
const Transaction = require('../models/Transaction');
const { isTransitionAllowed } = require('../utils/statusTransitions');
const { addCreditToBloggerWallet, processWithdrawalApproval } = require('../utils/walletService');
const { query } = require('../config/database');

/**
 * Manager Controller
 * The workflow traffic controller - handles all approvals
 */

// ==================== DASHBOARD ====================

/**
 * @route   GET /api/manager/dashboard
 * @desc    Get dashboard statistics matching production layout
 * @access  Manager only
 * 
 * Shows ONLY truly pending items from active workflow:
 * - Pending Bloggers: status 5 with submit_url (blogger submitted, awaiting manager approval)
 * - Pending Teams: status 2 (team submitted to manager)
 * - Pending Writers: status 4 (writer submitted content)
 * - Status 8 = Credited (completed, don't show)
 */
const getDashboardStats = async (req, res, next) => {
    try {
        // Pending Approvals for Bloggers: status 5 with submit_url means blogger submitted work
        // and it's waiting for manager to verify and credit
        const pendingBloggersResult = await query(
            `SELECT COUNT(*) as count FROM new_order_process_details 
             WHERE status = 5 AND submit_url IS NOT NULL AND submit_url != ''`
        );

        // Pending Approvals Teams: status 2 with no further progression
        // Only show items that are genuinely waiting (not old data that progressed)
        const pendingTeamsResult = await query(
            `SELECT COUNT(DISTINCT nop.id) as count 
             FROM new_order_processes nop
             WHERE nop.status = 2 
             AND NOT EXISTS (
                 SELECT 1 FROM new_order_processes nop2 
                 WHERE nop2.new_order_id = nop.new_order_id 
                 AND nop2.status > 2
             )`
        );

        // Pending Approvals Writers: status 4 with no further progression
        const pendingWritersResult = await query(
            `SELECT COUNT(DISTINCT nop.id) as count 
             FROM new_order_processes nop
             WHERE nop.status = 4 
             AND NOT EXISTS (
                 SELECT 1 FROM new_order_processes nop2 
                 WHERE nop2.new_order_id = nop.new_order_id 
                 AND nop2.status > 4
             )`
        );

        // Rejected Orders Bloggers (status 11 = Rejected)
        const rejectedOrdersResult = await query(
            `SELECT COUNT(*) as count FROM new_order_process_details WHERE status = 11`
        );

        // Threads count
        const threadsResult = await query(
            `SELECT COUNT(*) as count FROM threads`
        );

        // Today's Pending Approvals For Bloggers: status 5 with submit_url (truly pending)
        const pendingBloggerApprovalsResult = await query(
            `SELECT 
                o.order_id,
                u.name as vendor_name,
                u.email as vendor_email,
                ns.root_domain as new_site,
                'Pending' as status,
                nopd.id as detail_id,
                nopd.submit_url,
                nopd.created_at
             FROM new_order_process_details nopd
             JOIN new_order_processes nop ON nopd.new_order_process_id = nop.id
             JOIN new_orders o ON nop.new_order_id = o.id
             LEFT JOIN users u ON nopd.vendor_id = u.id
             LEFT JOIN new_sites ns ON nopd.new_site_id = ns.id
             WHERE nopd.status = 5 AND nopd.submit_url IS NOT NULL AND nopd.submit_url != ''
             ORDER BY nopd.created_at DESC
             LIMIT 50`
        );

        res.json({
            stats: {
                pending_bloggers: parseInt(pendingBloggersResult.rows[0]?.count || 0),
                pending_teams: parseInt(pendingTeamsResult.rows[0]?.count || 0),
                pending_writers: parseInt(pendingWritersResult.rows[0]?.count || 0),
                rejected_orders: parseInt(rejectedOrdersResult.rows[0]?.count || 0),
                threads: parseInt(threadsResult.rows[0]?.count || 0)
            },
            pending_blogger_approvals: pendingBloggerApprovalsResult.rows
        });
    } catch (error) {
        next(error);
    }
};

// ==================== TASK MANAGEMENT ====================

/**
 * @route   GET /api/manager/tasks
 * @desc    Get tasks for manager review
 * @access  Manager only
 */
const getTasks = async (req, res, next) => {
    try {
        const { status } = req.query;

        const filters = {};
        if (status) filters.current_status = status;

        const tasks = await Task.findAll(filters);

        res.json({
            count: tasks.length,
            tasks
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   GET /api/manager/orders
 * @desc    Get all orders from new_orders table with pagination
 * @access  Manager only
 */
const getOrders = async (req, res, next) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 50;
        const offset = (page - 1) * limit;
        const { status, search } = req.query;

        // Get total count
        let countQuery = `SELECT COUNT(*) as total FROM new_orders`;
        const countResult = await query(countQuery);
        const total = parseInt(countResult.rows[0]?.total || 0);

        // Get orders with manager name joined
        let ordersQuery = `
            SELECT 
                o.id,
                o.order_id,
                o.client_name,
                o.client_website,
                o.no_of_links,
                o.order_type,
                o.order_package,
                o.message,
                o.category,
                o.new_order_status as status,
                CASE 
                    WHEN o.new_order_status = 1 THEN 'Pending'
                    WHEN o.new_order_status = 2 THEN 'In Progress'
                    WHEN o.new_order_status = 3 THEN 'With Writer'
                    WHEN o.new_order_status = 4 THEN 'With Blogger'
                    WHEN o.new_order_status = 5 THEN 'Completed'
                    WHEN o.new_order_status = 6 THEN 'Rejected'
                    ELSE 'Unknown'
                END as status_label,
                o.completed_tasks,
                o.team_id,
                m.name as manager_name,
                m.email as manager_email,
                o.created_at,
                o.updated_at
            FROM new_orders o
            LEFT JOIN users m ON o.manager_id = m.id
            ORDER BY o.created_at DESC
            LIMIT $1 OFFSET $2
        `;

        const ordersResult = await query(ordersQuery, [limit, offset]);

        res.json({
            total,
            page,
            limit,
            totalPages: Math.ceil(total / limit),
            count: ordersResult.rows.length,
            orders: ordersResult.rows
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   PATCH /api/manager/orders/:id
 * @desc    Update an existing order
 * @access  Manager only
 */
const updateOrder = async (req, res, next) => {
    try {
        const { id } = req.params;
        const {
            client_name, client_website, order_type, no_of_links, tat_deadline,
            niche_price, gp_price, notes, message, assigned_team_id
        } = req.body;

        // Check task exists
        const task = await Task.findById(id);
        if (!task) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'Order not found'
            });
        }

        // Build update query dynamically
        const updates = [];
        const values = [];
        let paramIndex = 1;

        if (client_name !== undefined) {
            updates.push(`client_name = $${paramIndex++}`);
            values.push(client_name);
        }
        if (client_website !== undefined) {
            updates.push(`client_website = $${paramIndex++}`);
            values.push(client_website);
        }
        if (order_type !== undefined) {
            updates.push(`order_type = $${paramIndex++}`);
            values.push(order_type);
        }
        if (no_of_links !== undefined) {
            updates.push(`no_of_links = $${paramIndex++}`);
            values.push(no_of_links);
        }
        if (tat_deadline !== undefined) {
            updates.push(`tat_deadline = $${paramIndex++}`);
            values.push(tat_deadline);
        }
        if (niche_price !== undefined) {
            updates.push(`niche_price = $${paramIndex++}`);
            values.push(niche_price);
        }
        if (gp_price !== undefined) {
            updates.push(`gp_price = $${paramIndex++}`);
            values.push(gp_price);
        }
        if (notes !== undefined) {
            updates.push(`notes = $${paramIndex++}`);
            values.push(notes);
        }
        if (message !== undefined) {
            updates.push(`message = $${paramIndex++}`);
            values.push(message);
        }
        if (assigned_team_id !== undefined) {
            updates.push(`assigned_team_id = $${paramIndex++}`);
            values.push(assigned_team_id);
        }

        if (updates.length === 0) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'No fields to update'
            });
        }

        // Add id as the last parameter
        values.push(id);

        // Direct update to new_orders table
        const updateQuery = `
            UPDATE new_orders 
            SET ${updates.join(', ')}, updated_at = NOW()
            WHERE id = $${paramIndex}
            RETURNING *
        `;

        const result = await query(updateQuery, values);

        res.json({
            message: 'Order updated successfully',
            order: result.rows[0]
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   GET /api/manager/pending-from-bloggers
 * @desc    Get orders pending from bloggers (submitted URLs awaiting verification)
 * @access  Manager only
 */
const getPendingFromBloggers = async (req, res, next) => {
    try {
        // Get all orders where blogger has submitted URL (status = 7)
        // These are pending manager verification
        const result = await query(
            `SELECT 
                nopd.id as detail_id,
                nopd.new_order_process_id as process_id,
                nopd.new_site_id as site_id,
                nopd.vendor_id,
                nopd.status as detail_status,
                nopd.submit_url,
                nopd.anchor,
                nopd.url as target_url,
                nopd.ourl as post_url,
                nopd.type as option,
                nopd.insert_after,
                nopd.statement,
                nopd.note as notes,
                nopd.upfront_payment,
                nopd.created_at,
                ns.root_domain,
                ns.da,
                ns.dr,
                ns.gp_price,
                ns.niche_edit_price as niche_price,
                v.name as vendor_name,
                v.email as vendor_email,
                nop.new_order_id as order_id,
                no.client_name,
                no.order_type,
                no.category
             FROM new_order_process_details nopd
             JOIN new_sites ns ON nopd.new_site_id = ns.id
             JOIN new_order_processes nop ON nopd.new_order_process_id = nop.id
             JOIN new_orders no ON nop.new_order_id = no.id
             LEFT JOIN users v ON nopd.vendor_id = v.id
             WHERE nopd.status = 7
             ORDER BY nopd.updated_at DESC`
        );

        // Map to frontend format matching the screenshot
        const orders = result.rows.map(row => ({
            id: row.detail_id,
            order_id: row.order_id,
            order_type: row.order_type,

            // Vendor/Blogger info
            vendor_id: row.vendor_id,
            vendor_name: row.vendor_name,
            vendor_email: row.vendor_email,

            // Site info
            root_domain: row.root_domain,
            new_site: row.root_domain,
            da: row.da,
            dr: row.dr,

            // Order details
            anchor: row.anchor,
            target_url: row.target_url,
            post_url: row.post_url,
            option: row.option,
            insert_after: row.insert_after,
            statement: row.statement,
            notes: row.notes,

            // Submitted URL from blogger
            submitted_url: row.submit_url,
            submit_url: row.submit_url,

            // Status
            status: 'Accepted',
            detail_status: row.detail_status,

            created_at: row.created_at
        }));

        res.json({
            count: orders.length,
            orders
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   GET /api/manager/rejected-orders
 * @desc    Get rejected blogger orders from new_order_process_details with pagination
 * @access  Manager only
 */
const getRejectedOrders = async (req, res, next) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const offset = (page - 1) * limit;

        // Get total count (status 7 = submitted/pending rejection, status 11 = rejected)
        const countResult = await query(
            `SELECT COUNT(*) as total FROM new_order_process_details WHERE status IN (7, 11)`
        );
        const total = parseInt(countResult.rows[0]?.total || 0);

        // Get rejected orders with vendor and site info
        const ordersResult = await query(
            `SELECT 
                nopd.id,
                o.order_id,
                o.client_name,
                o.order_type,
                u.name as blogger_name,
                u.email as blogger_email,
                ns.root_domain as website_url,
                nopd.reject_reason as rejection_reason,
                CASE WHEN nopd.status = 7 THEN 'Pending Review' ELSE 'Rejected' END as status_label,
                nopd.status,
                nopd.created_at,
                nopd.updated_at
             FROM new_order_process_details nopd
             JOIN new_order_processes nop ON nopd.new_order_process_id = nop.id
             JOIN new_orders o ON nop.new_order_id = o.id
             LEFT JOIN users u ON nopd.vendor_id = u.id
             LEFT JOIN new_sites ns ON nopd.new_site_id = ns.id
             WHERE nopd.status IN (7, 11)
             ORDER BY nopd.updated_at DESC
             LIMIT $1 OFFSET $2`,
            [limit, offset]
        );

        res.json({
            total,
            page,
            limit,
            totalPages: Math.ceil(total / limit),
            count: ordersResult.rows.length,
            orders: ordersResult.rows
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   GET /api/manager/orders/:id/details
 * @desc    Get complete order workflow details including processes and blogger assignments
 * @access  Manager only
 */
const getOrderDetails = async (req, res, next) => {
    try {
        const { id } = req.params;

        // Get order basic info
        const orderResult = await query(
            `SELECT 
                o.id,
                o.order_id,
                o.client_name,
                o.client_website,
                o.no_of_links,
                o.order_type,
                o.order_package,
                o.message,
                o.category,
                o.new_order_status,
                o.created_at,
                o.updated_at,
                m.name as manager_name,
                m.email as manager_email
             FROM new_orders o
             LEFT JOIN users m ON o.manager_id = m.id
             WHERE o.id = $1`,
            [id]
        );

        if (orderResult.rows.length === 0) {
            return res.status(404).json({ error: 'Order not found' });
        }

        const order = orderResult.rows[0];

        // Get the process record for this order
        const processResult = await query(
            `SELECT 
                nop.id,
                nop.new_order_id,
                nop.team_id,
                nop.writer_id,
                nop.manager_id,
                nop.note,
                nop.doc_urls,
                nop.upload_doc_file,
                nop.status,
                nop.insert_after,
                nop.statement,
                nop.created_at,
                nop.updated_at,
                t.name as team_name,
                t.email as team_email,
                w.name as writer_name,
                w.email as writer_email
             FROM new_order_processes nop
             LEFT JOIN users t ON nop.team_id = t.id
             LEFT JOIN users w ON nop.writer_id = w.id
             WHERE nop.new_order_id = $1
             ORDER BY nop.created_at DESC
             LIMIT 1`,
            [id]
        );

        const process = processResult.rows[0] || null;

        // Get all detail records (blogger assignments)
        let processDetails = [];
        if (process) {
            const detailsResult = await query(
                `SELECT 
                    nopd.id,
                    nopd.new_order_process_id,
                    nopd.new_site_id,
                    nopd.url,
                    nopd.price,
                    nopd.anchor,
                    nopd.title,
                    nopd.doc_urls,
                    nopd.upload_doc_file,
                    nopd.insert_after,
                    nopd.statement,
                    nopd.note,
                    nopd.vendor_id,
                    nopd.status,
                    CASE 
                        WHEN nopd.status IS NULL THEN 'Pending'
                        WHEN nopd.status = 5 THEN 'Assigned'
                        WHEN nopd.status = 7 THEN 'Submitted'
                        WHEN nopd.status = 8 THEN 'Completed'
                        WHEN nopd.status = 10 THEN 'Archived'
                        WHEN nopd.status = 11 THEN 'Rejected'
                        ELSE 'Pending'
                    END as status_label,
                    nopd.submit_url,
                    nopd.ourl,
                    nopd.type,
                    nopd.new_note,
                    nopd.verify,
                    nopd.reject_reason,
                    nopd.created_at,
                    nopd.updated_at,
                    ns.root_domain as website,
                    ns.da,
                    ns.dr,
                    ns.niche_edit_price,
                    ns.gp_price,
                    b.name as blogger_name,
                    b.email as blogger_email
                 FROM new_order_process_details nopd
                 LEFT JOIN new_sites ns ON nopd.new_site_id = ns.id
                 LEFT JOIN users b ON nopd.vendor_id = b.id
                 WHERE nopd.new_order_process_id = $1
                 ORDER BY nopd.created_at ASC`,
                [process.id]
            );
            processDetails = detailsResult.rows;
        }

        // Reconstruct workflow steps based on available data
        const reconstructedProcesses = [];

        // Step 1: Team Assignment (status=1)
        if (process && process.team_id) {
            reconstructedProcesses.push({
                id: process.id,
                status: 1,
                status_label: 'Team Assigned',
                team_id: process.team_id,
                team_name: process.team_name,
                team_email: process.team_email,
                created_at: process.created_at,
                blogger_assignments: processDetails.map(d => ({
                    id: d.id,
                    website: d.website,
                    da: d.da,
                    dr: d.dr,
                    price: d.price || d.niche_edit_price || d.gp_price || 0
                }))
            });
        }

        // Step 2: Writer Assignment (status=2)
        if (process && process.writer_id) {
            reconstructedProcesses.push({
                id: process.id,
                status: 2,
                status_label: 'Writer Assigned',
                writer_id: process.writer_id,
                writer_name: process.writer_name,
                writer_email: process.writer_email,
                created_at: process.created_at,
                blogger_assignments: processDetails.map(d => ({
                    id: d.id,
                    website: d.website,
                    anchor: d.anchor,
                    ourl: d.url || d.ourl,
                    title: d.title
                }))
            });
        }

        // Step 3: Writer Submitted (status=3)
        const writerSubmitted = processDetails.some(d => d.doc_urls || d.insert_after || d.statement);
        if (process && process.writer_id && writerSubmitted) {
            reconstructedProcesses.push({
                id: process.id,
                status: 3,
                status_label: 'Writer Submitted',
                created_at: process.updated_at || process.created_at,
                blogger_assignments: processDetails.map(d => ({
                    id: d.id,
                    website: d.website,
                    anchor: d.anchor,
                    ourl: d.url || d.ourl,
                    doc_urls: d.doc_urls,
                    insert_after: d.insert_after,
                    statement: d.statement
                }))
            });
        }

        // Step 4/5: Pushed to Blogger (status=5 to match frontend)
        const hasBloggerAssignments = processDetails.some(d => d.vendor_id);
        if (hasBloggerAssignments) {
            reconstructedProcesses.push({
                id: process.id,
                status: 5,
                status_label: 'Blogger Pushed',
                created_at: process.updated_at || process.created_at,
                blogger_assignments: processDetails.map(d => ({
                    id: d.id,
                    website: d.website,
                    anchor: d.anchor,
                    ourl: d.url || d.ourl,
                    doc_urls: d.doc_urls,
                    blogger_name: d.blogger_name,
                    blogger_email: d.blogger_email,
                    status: d.status,
                    status_label: d.status_label,
                    submit_url: d.submit_url,
                    updated_at: d.updated_at
                }))
            });
        }

        // Calculate counts
        const completedCount = processDetails.filter(d => d.status === 8).length;
        const pendingCount = processDetails.filter(d => !d.submit_url && d.status !== 8 && d.status !== 11).length;
        const rejectedCount = processDetails.filter(d => d.status === 11).length;
        const submittedCount = processDetails.filter(d => d.submit_url).length;

        let currentStatus = 'Not Started';
        if (completedCount === processDetails.length && processDetails.length > 0) {
            currentStatus = 'Completed';
        } else if (hasBloggerAssignments) {
            currentStatus = 'Blogger Pushed';
        } else if (writerSubmitted) {
            currentStatus = 'Writer Submitted';
        } else if (process?.writer_id) {
            currentStatus = 'Writer Assigned';
        } else if (process?.team_id) {
            currentStatus = 'Team Assigned';
        }

        res.json({
            order: {
                ...order,
                current_workflow_status: currentStatus,
                current_workflow_status_code: process?.status || 0
            },
            processes: reconstructedProcesses,
            total_blogger_assignments: processDetails.length,
            submitted_count: submittedCount,
            pending_count: pendingCount,
            rejected_count: rejectedCount,
            completed_count: completedCount
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   POST /api/manager/orders
 * @desc    Create new order (WORKFLOW STEP 1: Manager pushes work to Team)
 * @access  Manager only
 */
const createOrder = async (req, res, next) => {
    try {
        const {
            client_name, order_type, no_of_links, tat_deadline,
            niche_price, gp_price, notes, assigned_team_id,
            // New Manager Panel fields
            manual_order_id, client_website, fc, order_package, category,
            post_url  // Post URL for Niche Edit orders
        } = req.body;

        // Validation
        if (!client_name) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Client name is required'
            });
        }

        // If team is assigned, verify they exist
        if (assigned_team_id) {
            const teamCheck = await query(
                "SELECT id, role FROM users WHERE id = $1 AND LOWER(role) = 'team'",
                [assigned_team_id]
            );

            if (teamCheck.rows.length === 0) {
                return res.status(400).json({
                    error: 'Validation Error',
                    message: 'Invalid team member ID or user is not a team member'
                });
            }
        }

        const task = await Task.create({
            created_by: req.user.id,
            manager_id: req.user.id,
            client_name,
            order_type: order_type || 'Guest Post',
            no_of_links: no_of_links || 1,
            tat_deadline: tat_deadline || null,
            niche_price: niche_price || 0,
            gp_price: gp_price || 0,
            notes,
            assigned_team_id,
            current_status: assigned_team_id ? 'PENDING_MANAGER_APPROVAL_1' : 'DRAFT',
            // New Manager Panel fields
            manual_order_id: manual_order_id || null,
            client_website: client_website || null,
            fc: fc || false,
            order_package: order_package || null,
            category: category || null,
            post_url: post_url || null  // Post URL for Niche Edit
        });

        // Emit real-time event for order creation
        const io = req.app.get('io');
        if (io) {
            const socketEvents = require('../utils/socketEvents');
            socketEvents.emitOrderCreated(io, task);
        }

        res.status(201).json({
            message: 'Order created successfully',
            task
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   PATCH /api/manager/tasks/:id/assign-team
 * @desc    Assign task to Team member
 * @access  Manager only
 */
const assignToTeam = async (req, res, next) => {
    try {
        const { id } = req.params;
        const { team_id } = req.body;

        // Validation
        if (!team_id) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Team member ID is required'
            });
        }

        // Get current task
        const task = await Task.findById(id);
        if (!task) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'Task not found'
            });
        }

        // Verify team member exists
        const teamCheck = await query(
            "SELECT id, role FROM users WHERE id = $1 AND role = 'Team'",
            [team_id]
        );

        if (teamCheck.rows.length === 0) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Invalid team member ID or user is not a team member'
            });
        }

        // Assign to team
        const updatedTask = await Task.assignToTeam(id, team_id);

        res.json({
            message: 'Task assigned to team member successfully',
            task: updatedTask
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   GET /api/manager/tasks/:id
 * @desc    Get specific task
 * @access  Manager only
 */
const getTaskById = async (req, res, next) => {
    try {
        const { id } = req.params;
        const task = await Task.findById(id);

        if (!task) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'Task not found'
            });
        }

        res.json({ task });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   PATCH /api/manager/tasks/:id/assign
 * @desc    Approval 1: Assign task to writer
 * @access  Manager only
 */
const assignToWriter = async (req, res, next) => {
    try {
        const { id } = req.params;
        const { writer_id, instructions, website_details } = req.body;

        // Validation
        if (!writer_id) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Writer ID is required'
            });
        }

        // Get current task
        const task = await Task.findById(id);
        if (!task) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'Task not found'
            });
        }

        // Validate status transition
        if (!isTransitionAllowed(task.current_status, 'ASSIGNED_TO_WRITER')) {
            return res.status(400).json({
                error: 'Invalid Transition',
                message: `Cannot assign to writer from status: ${task.current_status} `
            });
        }

        // Verify writer exists and has Writer role
        const writerCheck = await query(
            "SELECT id, role FROM users WHERE id = $1 AND LOWER(role) = 'writer'",
            [writer_id]
        );

        if (writerCheck.rows.length === 0) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Invalid writer ID or user is not a writer'
            });
        }

        // Update website details if provided - use new_order_process_details table
        if (website_details && Array.isArray(website_details) && website_details.length > 0) {
            console.log('Updating website details:', website_details);

            const updatePromises = website_details.map(detail => {
                const { id: detail_id, target_url, anchor_text, article_title, upfront_payment, paypal_id } = detail;
                console.log(`Updating detail ID ${detail_id}: `, { target_url, anchor_text, article_title, upfront_payment, paypal_id });

                return query(
                    `UPDATE new_order_process_details 
                     SET url = $1,
    anchor = $2,
    title = $3,
    upfront_payment = $4,
    paypal_email = $5,
    updated_at = CURRENT_TIMESTAMP
                     WHERE id = $6`,
                    [target_url, anchor_text, article_title, upfront_payment ? 1 : 0, paypal_id || null, detail_id]
                );
            });

            await Promise.all(updatePromises);
        }

        // Assign to writer
        const updatedTask = await Task.assignToWriter(id, writer_id, instructions);

        res.json({
            message: 'Task assigned to writer successfully',
            task: updatedTask
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   PATCH /api/manager/tasks/:id/approve-content
 * @desc    Approval 2: Approve content and assign to blogger
 * @access  Manager only
 */
const approveContent = async (req, res, next) => {
    try {
        const { id } = req.params;
        const { blogger_id } = req.body;

        // Validation
        if (!blogger_id) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Blogger ID is required'
            });
        }

        // Get current task
        const task = await Task.findById(id);
        if (!task) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'Task not found'
            });
        }

        // Validate status transition
        if (!isTransitionAllowed(task.current_status, 'ASSIGNED_TO_BLOGGER')) {
            return res.status(400).json({
                error: 'Invalid Transition',
                message: `Cannot assign to blogger from status: ${task.current_status} `
            });
        }

        // Verify blogger exists
        const bloggerCheck = await query(
            "SELECT id, role FROM users WHERE id = $1 AND role = 'Blogger'",
            [blogger_id]
        );

        if (bloggerCheck.rows.length === 0) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Invalid blogger ID or user is not a blogger'
            });
        }

        // Assign to blogger
        const updatedTask = await Task.assignToBlogger(id, blogger_id);

        // Create notification for blogger
        await query(
            `INSERT INTO notifications(user_id, title, message, type, related_task_id, action_url)
VALUES($1, $2, $3, $4, $5, $6)`,
            [
                blogger_id,
                'New Order Pushed',
                `Order #${id} has been assigned to you`,
                'order_pushed',
                id,
                '/blogger/orders'
            ]
        );

        res.json({
            message: 'Content approved and assigned to blogger',
            task: updatedTask
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   PATCH /api/manager/tasks/:id/return-to-writer
 * @desc    Return content to writer for revisions
 * @access  Manager only
 */
const returnToWriter = async (req, res, next) => {
    try {
        const { id } = req.params;
        const { notes } = req.body;

        const task = await Task.findById(id);
        if (!task) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'Task not found'
            });
        }

        if (!isTransitionAllowed(task.current_status, 'ASSIGNED_TO_WRITER')) {
            return res.status(400).json({
                error: 'Invalid Transition',
                message: 'Cannot return to writer from current status'
            });
        }

        const updatedTask = await Task.updateStatus(id, 'ASSIGNED_TO_WRITER', { notes });

        res.json({
            message: 'Task returned to writer for revisions',
            task: updatedTask
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   PATCH /api/manager/tasks/:id/finalize
 * @desc    Approval 3: Finalize task and credit blogger
 * @access  Manager only
 */
const finalizeTask = async (req, res, next) => {
    try {
        const { id } = req.params;

        // Get current task
        const task = await Task.findById(id);
        if (!task) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'Task not found'
            });
        }

        // Validate status
        if (task.current_status !== 'PENDING_FINAL_CHECK') {
            return res.status(400).json({
                error: 'Invalid Transition',
                message: 'Task is not ready for finalization'
            });
        }

        // Get order detail ID and payment amount from new_order_process_details
        // This links the wallet_history entry to the site for root_domain display
        const orderDetailResult = await query(
            `SELECT nopd.id as order_detail_id, nopd.price, nopd.vendor_id
             FROM new_order_process_details nopd
             JOIN new_order_processes nop ON nopd.new_order_process_id = nop.id
             WHERE nop.new_order_id = $1 AND nopd.vendor_id = $2
             ORDER BY nopd.id DESC LIMIT 1`,
            [id, task.assigned_blogger_id]
        );

        let orderDetailId = null;
        let paymentAmount = 50.00; // Default

        if (orderDetailResult.rows.length > 0) {
            orderDetailId = orderDetailResult.rows[0].order_detail_id;
            paymentAmount = parseFloat(orderDetailResult.rows[0].price) || 50.00;
        } else {
            // Fallback: Get default payment from config
            const configResult = await query(
                "SELECT config_value FROM system_config WHERE config_key = 'default_post_payment'"
            );
            if (configResult.rows.length > 0) {
                paymentAmount = parseFloat(configResult.rows[0].config_value);
            }
        }

        // Credit blogger wallet with order detail linkage for root_domain in wallet history
        await addCreditToBloggerWallet(task.assigned_blogger_id, paymentAmount, orderDetailId);

        // Update order detail status to 8 (credited)
        if (orderDetailId) {
            await query(
                'UPDATE new_order_process_details SET status = 8, updated_at = CURRENT_TIMESTAMP WHERE id = $1',
                [orderDetailId]
            );
        }

        // Mark task as completed and credited
        const updatedTask = await Task.markAsCompleted(id, paymentAmount);

        res.json({
            message: 'Task finalized and blogger credited successfully',
            task: updatedTask,
            payment_credited: paymentAmount
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   PATCH /api/manager/tasks/:id/reject
 * @desc    Reject task
 * @access  Manager only
 */
const rejectTask = async (req, res, next) => {
    try {
        const { id } = req.params;
        const { rejection_reason } = req.body;

        if (!rejection_reason) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Rejection reason is required'
            });
        }

        const updatedTask = await Task.reject(id, rejection_reason);

        res.json({
            message: 'Task rejected',
            task: updatedTask
        });
    } catch (error) {
        next(error);
    }
};

// ==================== WITHDRAWAL MANAGEMENT ====================

/**
 * @route   GET /api/manager/withdrawals
 * @desc    Get all withdrawal requests
 * @access  Manager only
 */
const getWithdrawals = async (req, res, next) => {
    try {
        const { status } = req.query;

        const filters = {};
        if (status) filters.status = status;

        const withdrawals = await Transaction.findAll(filters);

        res.json({
            count: withdrawals.length,
            withdrawals
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   PATCH /api/manager/withdrawals/:id/approve
 * @desc    Approve withdrawal request
 * @access  Manager only
 */
const approveWithdrawal = async (req, res, next) => {
    try {
        const { id } = req.params;

        const withdrawal = await processWithdrawalApproval(id, req.user.id);

        res.json({
            message: 'Withdrawal approved and processed',
            withdrawal
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   PATCH /api/manager/withdrawals/:id/reject
 * @desc    Reject withdrawal request
 * @access  Manager only
 */
const rejectWithdrawal = async (req, res, next) => {
    try {
        const { id } = req.params;
        const { rejection_reason } = req.body;

        if (!rejection_reason) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Rejection reason is required'
            });
        }

        const withdrawal = await Transaction.reject(id, req.user.id, rejection_reason);

        res.json({
            message: 'Withdrawal request rejected',
            withdrawal
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   GET /api/manager/pending-from-teams
 * @desc    Get orders pending approval from Team members
 * @access  Manager only
 */
const getPendingFromTeams = async (req, res, next) => {
    try {
        const orders = await Task.findAll({
            manager_id: req.user.id,
            current_status: 'PENDING_MANAGER_APPROVAL_1'
        });

        res.json({
            count: orders.length,
            orders
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   GET /api/manager/pending-from-writers
 * @desc    Get orders pending content approval from Writers
 * @access  Manager only
 */
const getPendingFromWriters = async (req, res, next) => {
    try {
        const allOrders = await Task.findAll({ manager_id: req.user.id });
        const orders = allOrders.filter(o =>
            o.current_status === 'SUBMITTED_TO_MANAGER' ||
            o.current_status === 'PENDING_MANAGER_APPROVAL_2'
        );

        res.json({
            count: orders.length,
            orders
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   PATCH /api/manager/tasks/:id/approve-team
 * @desc    Approve team's website selection
 * @access  Manager only
 */
const approveTeamSubmission = async (req, res, next) => {
    try {
        const { id } = req.params;

        const task = await Task.findById(id);
        if (!task) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'Task not found'
            });
        }

        // Move to next status - ready for writer assignment
        const updated = await Task.updateStatus(id, 'PENDING_MANAGER_APPROVAL_1', {});

        res.json({
            message: 'Team submission approved',
            task: updated
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   PATCH /api/manager/tasks/:id/reject-team
 * @desc    Reject team's website selection
 * @access  Manager only
 */
const rejectTeamSubmission = async (req, res, next) => {
    try {
        const { id } = req.params;
        const { reason } = req.body;

        const task = await Task.findById(id);
        if (!task) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'Task not found'
            });
        }

        // Clear website selection and send back to team
        const updated = await Task.updateStatus(id, 'PENDING_MANAGER_APPROVAL_1', {
            website_id: null,
            rejection_reason: reason
        });

        res.json({
            message: 'Team submission rejected',
            task: updated
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   GET /api/manager/team-members
 * @desc    Get all team members for assignment dropdown
 * @access  Manager only
 */
const getTeamMembers = async (req, res, next) => {
    try {
        const result = await query(
            "SELECT id, name, name as username, email FROM users WHERE LOWER(role) = 'team' ORDER BY name"
        );

        res.json({
            count: result.rows.length,
            users: result.rows
        });
    } catch (error) {
        next(error);
    }
};

const getWriters = async (req, res, next) => {
    try {
        const result = await query(
            "SELECT id, name as username, email FROM users WHERE LOWER(role) = 'writer' ORDER BY name"
        );

        res.json({
            count: result.rows.length,
            users: result.rows
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   GET /api/manager/bloggers
 * @desc    Get all bloggers for assignment dropdown
 * @access  Manager only
 */
const getBloggers = async (req, res, next) => {
    try {
        const result = await query(
            "SELECT id, username, email FROM users WHERE role = 'Blogger' AND is_active = true ORDER BY username"
        );

        res.json({
            count: result.rows.length,
            bloggers: result.rows
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   GET /api/manager/websites
 * @desc    Get all websites/sites for manager view (with pagination)
 * @access  Manager only
 */
const getWebsites = async (req, res, next) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 50;
        const offset = (page - 1) * limit;

        // Get total count (only active sites)
        const countResult = await query(
            `SELECT COUNT(*) as total FROM new_sites
WHERE(delete_site IS NULL OR delete_site = 0) AND site_status = '1'`
        );
        const total = parseInt(countResult.rows[0].total);
        const totalPages = Math.ceil(total / limit);

        // Get paginated sites (only active sites) with LO Created from orders
        const result = await query(
            `SELECT ns.id, ns.root_domain, ns.niche, ns.category,
    ns.da, ns.dr, ns.rd, ns.spam_score, ns.traffic_source as traffic,
    ns.gp_price, ns.niche_edit_price, ns.deal_cbd_casino,
    ns.email, ns.site_status, ns.website_status,
    ns.fc_gp, ns.fc_ne, ns.website_niche, ns.sample_url, ns.href_url,
    ns.paypal_id, ns.skype, ns.whatsapp, ns.country_source,
    ns.created_at, ns.updated_at,
    (SELECT MAX(nopd.created_at) FROM new_order_process_details nopd WHERE nopd.new_site_id = ns.id) as lo_created_at
             FROM new_sites ns
WHERE(ns.delete_site IS NULL OR ns.delete_site = 0) AND ns.site_status = '1'
             ORDER BY ns.created_at DESC
             LIMIT $1 OFFSET $2`,
            [limit, offset]
        );

        res.json({
            sites: result.rows,
            pagination: {
                page,
                limit,
                total,
                totalPages
            }
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   POST /api/manager/tasks/:id/push-to-bloggers
 * @desc    Push task to bloggers - auto-routes each site to its owner
 * @access  Manager only
 * 
 * This function automatically assigns each selected website to its owner (vendor).
 * The vendor_id is set to the uploaded_user_id from new_sites table.
 */
const pushToBloggers = async (req, res, next) => {
    try {
        const { id } = req.params; // order ID

        // Get the latest process for this order
        const processResult = await query(
            `SELECT id FROM new_order_processes WHERE new_order_id = $1 ORDER BY id DESC LIMIT 1`,
            [id]
        );

        if (!processResult.rows[0]) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'No process found for this order'
            });
        }

        const processId = processResult.rows[0].id;

        // Get all process details and their site owners
        const details = await query(
            `SELECT
nopd.id as detail_id,
    nopd.new_site_id,
    ns.uploaded_user_id as site_owner_id,
    ns.root_domain,
    u.name as owner_name,
    u.email as owner_email
             FROM new_order_process_details nopd
             JOIN new_sites ns ON nopd.new_site_id = ns.id
             LEFT JOIN users u ON ns.uploaded_user_id = u.id
             WHERE nopd.new_order_process_id = $1`,
            [processId]
        );

        if (details.rows.length === 0) {
            return res.status(400).json({
                error: 'No websites found',
                message: 'No websites are selected for this order'
            });
        }

        // Auto-route each site to its owner
        const pushedTasks = [];
        for (const detail of details.rows) {
            // Update the detail with vendor_id = site owner
            await query(
                `UPDATE new_order_process_details 
                 SET vendor_id = $1, status = 5, updated_at = CURRENT_TIMESTAMP
                 WHERE id = $2`,
                [detail.site_owner_id, detail.detail_id]
            );

            // Create notification for the vendor/blogger
            try {
                await query(
                    `INSERT INTO notifications(id, type, notifiable_type, notifiable_id, data, created_at, updated_at)
VALUES(gen_random_uuid(), 'order_assigned', 'App\\Models\\User', $1, $2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)`,
                    [
                        detail.site_owner_id,
                        JSON.stringify({
                            message: `New task assigned for ${detail.root_domain}`,
                            order_id: id,
                            detail_id: detail.detail_id
                        })
                    ]
                );
            } catch (notifError) {
                console.log('Notification creation skipped (table might not exist):', notifError.message);
            }

            pushedTasks.push({
                detail_id: detail.detail_id,
                root_domain: detail.root_domain,
                vendor_id: detail.site_owner_id,
                vendor_name: detail.owner_name,
                vendor_email: detail.owner_email
            });
        }

        // Update process status to 5 (assigned to vendors)
        await query(
            `UPDATE new_order_processes SET status = 5, updated_at = CURRENT_TIMESTAMP WHERE id = $1`,
            [processId]
        );

        // Update order status
        await query(
            `UPDATE new_orders SET new_order_status = 5, updated_at = CURRENT_TIMESTAMP WHERE id = $1`,
            [id]
        );

        // Emit socket event for real-time updates
        const io = req.app.get('io');
        if (io) {
            pushedTasks.forEach(task => {
                io.emit(`vendor_${task.vendor_id} _new_task`, {
                    message: `New task assigned for ${task.root_domain}`,
                    order_id: id
                });
            });
        }

        res.json({
            message: 'Tasks pushed to bloggers successfully',
            pushed_count: pushedTasks.length,
            tasks: pushedTasks
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   GET /api/manager/blogger-submissions/:id
 * @desc    Get full detail of a blogger's submission for manager review
 * @access  Manager only
 * 
 * id = new_order_process_details.id (detail_id)
 */
const getBloggerSubmissionDetail = async (req, res, next) => {
    try {
        const { id } = req.params;

        const result = await query(
            `SELECT 
                nopd.id as detail_id,
        nopd.new_order_process_id as process_id,
        nopd.new_site_id as site_id,
        nopd.vendor_id,
        nopd.status as detail_status,
        nopd.submit_url,
        nopd.anchor,
        nopd.url as target_url,
        nopd.ourl as post_url,
        nopd.type as option,
        nopd.insert_after,
        nopd.statement,
        nopd.note as notes,
        nopd.upfront_payment,
        nopd.doc_urls,
        nopd.title,
        nopd.upload_doc_file,
        nopd.created_at,
        nopd.updated_at,
        ns.root_domain,
        ns.da,
        ns.dr,
        ns.gp_price,
        ns.niche_edit_price as niche_price,
        v.name as vendor_name,
        v.email as vendor_email,
        nop.new_order_id as order_id,
        no.client_name,
        no.order_type,
        no.category,
        no.message as order_notes
             FROM new_order_process_details nopd
             JOIN new_sites ns ON nopd.new_site_id = ns.id
             JOIN new_order_processes nop ON nopd.new_order_process_id = nop.id
             JOIN new_orders no ON nop.new_order_id = no.id
             LEFT JOIN users v ON nopd.vendor_id = v.id
             WHERE nopd.id = $1`,
            [id]
        );

        if (result.rows.length === 0) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'Submission not found'
            });
        }

        const row = result.rows[0];
        const detail = {
            id: row.detail_id,
            order_id: row.order_id,

            // Vendor/Blogger info
            vendor_id: row.vendor_id,
            vendor_name: row.vendor_name,
            vendor_email: row.vendor_email,

            // Site info
            root_domain: row.root_domain,
            da: row.da,
            dr: row.dr,
            price: row.niche_price || row.gp_price || 0,

            // Order details (matching Screenshot 3)
            post_url: row.post_url,
            insert_after: row.insert_after,
            insert_statement: row.statement,
            anchor: row.anchor,
            url: row.target_url,

            // Submission
            submitted_url: row.submit_url,
            submit_url: row.submit_url,
            link_verification: 'Not Verified', // TODO: Add actual verification
            upfront_payment: row.upfront_payment || false,

            // Order info
            order_type: row.order_type,
            client_name: row.client_name,
            notes: row.notes,

            // Doc/Content fields
            doc_urls: row.doc_urls,
            title: row.title,
            upload_doc_file: row.upload_doc_file,

            // Status
            detail_status: row.detail_status,

            created_at: row.created_at,
            updated_at: row.updated_at
        };

        res.json({ detail });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   POST /api/manager/blogger-submissions/:id/finalize
 * @desc    Finalize blogger submission - mark as complete and credit blogger
 * @access  Manager only
 * 
 * id = new_order_process_details.id (detail_id)
 */
const finalizeFromBlogger = async (req, res, next) => {
    try {
        const { id } = req.params;
        const { credit_amount } = req.body;

        // Get the detail first
        const detailResult = await query(
            `SELECT nopd.*, ns.root_domain, ns.niche_edit_price as niche_price, ns.gp_price, v.name as vendor_name
             FROM new_order_process_details nopd
             JOIN new_sites ns ON nopd.new_site_id = ns.id
             LEFT JOIN users v ON nopd.vendor_id = v.id
             WHERE nopd.id = $1`,
            [id]
        );

        if (detailResult.rows.length === 0) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'Submission not found'
            });
        }

        const detail = detailResult.rows[0];

        // Update status to 8 (completed/credited)
        await query(
            `UPDATE new_order_process_details 
             SET status = 8, updated_at = CURRENT_TIMESTAMP
             WHERE id = $1`,
            [id]
        );

        // Credit the blogger's wallet (using the site price or provided amount)
        // Parse prices properly since they may be VARCHAR with "N/A" or other non-numeric values
        let amount = 0;
        if (credit_amount) {
            amount = parseFloat(credit_amount);
        } else if (detail.niche_price && !isNaN(parseFloat(detail.niche_price))) {
            amount = parseFloat(detail.niche_price);
        } else if (detail.gp_price && !isNaN(parseFloat(detail.gp_price))) {
            amount = parseFloat(detail.gp_price);
        }

        if (amount > 0 && detail.vendor_id) {
            try {
                await addCreditToBloggerWallet(detail.vendor_id, amount, id); // Pass order_detail_id for wallet history linkage
            } catch (walletError) {
                console.log('Wallet credit skipped:', walletError.message);
            }
        }

        // Check if all details for this process are completed
        const processCheck = await query(
            `SELECT COUNT(*) as pending FROM new_order_process_details 
             WHERE new_order_process_id = $1 AND status != 8`,
            [detail.new_order_process_id]
        );

        // If all details are complete, update process status
        if (parseInt(processCheck.rows[0].pending) === 0) {
            await query(
                `UPDATE new_order_processes SET status = 8, updated_at = CURRENT_TIMESTAMP 
                 WHERE id = $1`,
                [detail.new_order_process_id]
            );
        }

        res.json({
            message: 'Order finalized and blogger credited!',
            detail_id: id,
            root_domain: detail.root_domain,
            vendor_name: detail.vendor_name,
            credited_amount: amount
        });
    } catch (error) {
        next(error);
    }
};

/**
 * @route   POST /api/manager/blogger-submissions/:id/reject
 * @desc    Reject blogger submission - sends back to blogger with reason
 * @access  Manager only
 */
const rejectBloggerSubmission = async (req, res, next) => {
    try {
        const { id } = req.params;
        const { rejection_reason } = req.body;

        if (!rejection_reason) {
            return res.status(400).json({
                error: 'Validation Error',
                message: 'Rejection reason is required'
            });
        }

        // Check if detail exists
        const detailResult = await query(
            `SELECT nopd.*, ns.root_domain, u.name as vendor_name
             FROM new_order_process_details nopd
             LEFT JOIN new_sites ns ON nopd.new_site_id = ns.id
             LEFT JOIN users u ON nopd.vendor_id = u.id
             WHERE nopd.id = $1`,
            [id]
        );

        if (detailResult.rows.length === 0) {
            return res.status(404).json({
                error: 'Not Found',
                message: 'Blogger submission not found'
            });
        }

        const detail = detailResult.rows[0];

        // Update status to 11 (Rejected) and store rejection reason
        await query(
            `UPDATE new_order_process_details 
             SET status = 11, reject_reason = $1, updated_at = CURRENT_TIMESTAMP 
             WHERE id = $2`,
            [rejection_reason, id]
        );

        res.json({
            message: 'Blogger submission rejected successfully',
            detail_id: id,
            root_domain: detail.root_domain,
            vendor_name: detail.vendor_name,
            rejection_reason
        });
    } catch (error) {
        next(error);
    }
};

module.exports = {
    getDashboardStats,
    getTasks,
    getTaskById,
    getOrders,
    updateOrder,
    getOrderDetails,
    getPendingFromBloggers,
    getPendingFromTeams,
    getPendingFromWriters,
    getRejectedOrders,
    createOrder,
    assignToTeam,
    assignToWriter,
    approveContent,
    approveTeamSubmission,
    rejectTeamSubmission,
    returnToWriter,
    finalizeTask,
    rejectTask,
    getWithdrawals,
    approveWithdrawal,
    rejectWithdrawal,
    getTeamMembers,
    getWriters,
    getBloggers,
    getWebsites,
    pushToBloggers,
    getBloggerSubmissionDetail,
    finalizeFromBlogger,
    rejectBloggerSubmission
};
