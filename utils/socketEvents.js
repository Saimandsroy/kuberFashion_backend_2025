/**
 * Socket.io Utility Module
 * Provides helper functions to emit real-time events
 */

/**
 * Emit order created event
 * @param {Object} io - Socket.io instance
 * @param {Object} order - The created order data
 */
const emitOrderCreated = (io, order) => {
    if (!io) return;
    io.to('orders-list').emit('order-created', {
        type: 'ORDER_CREATED',
        order,
        timestamp: new Date().toISOString()
    });
    console.log(`📡 Emitted order-created for order ${order.id}`);
};

/**
 * Emit order updated event
 * @param {Object} io - Socket.io instance
 * @param {Object} order - The updated order data
 */
const emitOrderUpdated = (io, order) => {
    if (!io) return;
    // Emit to orders list
    io.to('orders-list').emit('order-updated', {
        type: 'ORDER_UPDATED',
        order,
        timestamp: new Date().toISOString()
    });
    // Emit to specific order room
    io.to(`order-${order.id}`).emit('order-detail-updated', {
        type: 'ORDER_DETAIL_UPDATED',
        order,
        timestamp: new Date().toISOString()
    });
    console.log(`📡 Emitted order-updated for order ${order.id}`);
};

/**
 * Emit workflow status changed event
 * @param {Object} io - Socket.io instance
 * @param {number} orderId - Order ID
 * @param {string} newStatus - New workflow status
 * @param {Object} additionalData - Extra data about the change
 */
const emitWorkflowChanged = (io, orderId, newStatus, additionalData = {}) => {
    if (!io) return;
    const payload = {
        type: 'WORKFLOW_CHANGED',
        orderId,
        newStatus,
        ...additionalData,
        timestamp: new Date().toISOString()
    };
    io.to('orders-list').emit('workflow-changed', payload);
    io.to(`order-${orderId}`).emit('workflow-changed', payload);
    console.log(`📡 Emitted workflow-changed for order ${orderId}: ${newStatus}`);
};

/**
 * Emit blogger assignment event
 * @param {Object} io - Socket.io instance
 * @param {number} orderId - Order ID
 * @param {Object} assignment - Assignment details
 */
const emitBloggerAssigned = (io, orderId, assignment) => {
    if (!io) return;
    io.to(`order-${orderId}`).emit('blogger-assigned', {
        type: 'BLOGGER_ASSIGNED',
        orderId,
        assignment,
        timestamp: new Date().toISOString()
    });
    console.log(`📡 Emitted blogger-assigned for order ${orderId}`);
};

/**
 * Emit submit URL event (blogger submitted live link)
 * @param {Object} io - Socket.io instance
 * @param {number} orderId - Order ID
 * @param {Object} submission - Submission details
 */
const emitUrlSubmitted = (io, orderId, submission) => {
    if (!io) return;
    const payload = {
        type: 'URL_SUBMITTED',
        orderId,
        submission,
        timestamp: new Date().toISOString()
    };
    io.to('orders-list').emit('url-submitted', payload);
    io.to(`order-${orderId}`).emit('url-submitted', payload);
    console.log(`📡 Emitted url-submitted for order ${orderId}`);
};

module.exports = {
    emitOrderCreated,
    emitOrderUpdated,
    emitWorkflowChanged,
    emitBloggerAssigned,
    emitUrlSubmitted
};
