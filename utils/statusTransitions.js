/**
 * Workflow Status Transitions
 * Defines allowed status transitions in the workflow state machine
 */

const STATUS = {
    DRAFT: 'DRAFT',
    PENDING_MANAGER_APPROVAL_1: 'PENDING_MANAGER_APPROVAL_1',
    ASSIGNED_TO_WRITER: 'ASSIGNED_TO_WRITER',
    WRITING_IN_PROGRESS: 'WRITING_IN_PROGRESS',
    SUBMITTED_TO_MANAGER: 'SUBMITTED_TO_MANAGER',
    PENDING_MANAGER_APPROVAL_2: 'PENDING_MANAGER_APPROVAL_2',
    ASSIGNED_TO_BLOGGER: 'ASSIGNED_TO_BLOGGER',
    PUBLISHED_PENDING_VERIFICATION: 'PUBLISHED_PENDING_VERIFICATION',
    PENDING_FINAL_CHECK: 'PENDING_FINAL_CHECK',
    COMPLETED: 'COMPLETED',
    REJECTED: 'REJECTED',
    CREDITED: 'CREDITED'
};

/**
 * Map of allowed transitions
 * Key: Current Status
 * Value: Array of allowed next statuses
 */
const ALLOWED_TRANSITIONS = {
    [STATUS.DRAFT]: [STATUS.PENDING_MANAGER_APPROVAL_1],

    [STATUS.PENDING_MANAGER_APPROVAL_1]: [
        STATUS.ASSIGNED_TO_WRITER,
        STATUS.REJECTED
    ],

    [STATUS.ASSIGNED_TO_WRITER]: [
        STATUS.WRITING_IN_PROGRESS,
        STATUS.SUBMITTED_TO_MANAGER,
        STATUS.PENDING_MANAGER_APPROVAL_2
    ],

    [STATUS.WRITING_IN_PROGRESS]: [
        STATUS.SUBMITTED_TO_MANAGER,
        STATUS.PENDING_MANAGER_APPROVAL_2
    ],

    [STATUS.SUBMITTED_TO_MANAGER]: [
        STATUS.PENDING_MANAGER_APPROVAL_2,
        STATUS.ASSIGNED_TO_BLOGGER,
        STATUS.ASSIGNED_TO_WRITER, // Return to writer
        STATUS.REJECTED
    ],

    [STATUS.PENDING_MANAGER_APPROVAL_2]: [
        STATUS.ASSIGNED_TO_BLOGGER,
        STATUS.ASSIGNED_TO_WRITER, // Return to writer
        STATUS.REJECTED
    ],

    [STATUS.ASSIGNED_TO_BLOGGER]: [
        STATUS.PUBLISHED_PENDING_VERIFICATION,
        STATUS.PENDING_FINAL_CHECK
    ],

    [STATUS.PUBLISHED_PENDING_VERIFICATION]: [
        STATUS.PENDING_FINAL_CHECK
    ],

    [STATUS.PENDING_FINAL_CHECK]: [
        STATUS.COMPLETED,
        STATUS.ASSIGNED_TO_BLOGGER, // Return to blogger
        STATUS.REJECTED
    ],

    [STATUS.COMPLETED]: [
        STATUS.CREDITED
    ],

    [STATUS.CREDITED]: [], // Final state
    [STATUS.REJECTED]: []  // Final state
};

/**
 * Validate if a status transition is allowed
 * @param {string} currentStatus - Current task status
 * @param {string} newStatus - Desired new status
 * @returns {boolean} - True if transition is allowed
 */
const isTransitionAllowed = (currentStatus, newStatus) => {
    if (!ALLOWED_TRANSITIONS[currentStatus]) {
        return false;
    }

    return ALLOWED_TRANSITIONS[currentStatus].includes(newStatus);
};

/**
 * Get allowed next statuses for a given status
 * @param {string} currentStatus - Current task status
 * @returns {string[]} - Array of allowed next statuses
 */
const getAllowedNextStatuses = (currentStatus) => {
    return ALLOWED_TRANSITIONS[currentStatus] || [];
};

/**
 * Get status description
 */
const getStatusDescription = (status) => {
    const descriptions = {
        [STATUS.DRAFT]: 'Team member is drafting the task',
        [STATUS.PENDING_MANAGER_APPROVAL_1]: 'Waiting for manager to approve topic',
        [STATUS.ASSIGNED_TO_WRITER]: 'Assigned to writer, waiting to start',
        [STATUS.WRITING_IN_PROGRESS]: 'Writer is working on content',
        [STATUS.PENDING_MANAGER_APPROVAL_2]: 'Waiting for manager to approve content',
        [STATUS.ASSIGNED_TO_BLOGGER]: 'Assigned to blogger for publishing',
        [STATUS.PUBLISHED_PENDING_VERIFICATION]: 'Blogger has published, awaiting verification',
        [STATUS.PENDING_FINAL_CHECK]: 'Manager is verifying the published link',
        [STATUS.COMPLETED]: 'Task completed successfully',
        [STATUS.REJECTED]: 'Task was rejected',
        [STATUS.CREDITED]: 'Payment credited to blogger'
    };

    return descriptions[status] || 'Unknown status';
};

module.exports = {
    STATUS,
    ALLOWED_TRANSITIONS,
    isTransitionAllowed,
    getAllowedNextStatuses,
    getStatusDescription
};
