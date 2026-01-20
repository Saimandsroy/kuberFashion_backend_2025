const express = require('express');
const router = express.Router();
const adminController = require('../controllers/adminController');
const managerController = require('../controllers/managerController');
const { authenticate, authorize } = require('../middleware/auth');

// All routes require Admin role
router.use(authenticate, authorize('Admin'));

// ==================== ORDERS MANAGEMENT (reuse manager functions) ====================
router.get('/orders', managerController.getOrders);
router.get('/orders/:id/details', managerController.getOrderDetails);
router.patch('/orders/:id', managerController.updateOrder);

// ==================== USER MANAGEMENT ====================
router.get('/users', adminController.getAllUsers);
router.post('/users', adminController.createUser);
router.put('/users/:id', adminController.updateUser);
router.delete('/users/:id', adminController.deleteUser);

// ==================== WEBSITE MANAGEMENT ====================
router.get('/websites', adminController.getAllWebsites);
router.post('/websites', adminController.createWebsite);
router.post('/websites/upload', adminController.upload.single('file'), adminController.uploadWebsitesCSV);
router.put('/websites/:id', adminController.updateWebsite);
router.delete('/websites/:id', adminController.deleteWebsite);

// ==================== SITES EXCEL MANAGEMENT ====================
router.get('/sites/download-format', adminController.downloadSiteFormat);
router.post('/sites/upload-excel', adminController.upload.single('file'), adminController.uploadSitesExcel);

// ==================== STATISTICS ====================
router.get('/stats', adminController.getStatistics);

// ==================== TASKS (Read-only for admin overview) ====================
router.get('/tasks', adminController.getAllTasks);

// ==================== WITHDRAWALS (Read-only for admin overview) ====================
router.get('/withdrawals', adminController.getAllWithdrawals);

// ==================== PRICE CHARTS MANAGEMENT ====================
router.get('/price-charts', adminController.getAllPriceCharts);
router.post('/price-charts', adminController.createPriceChart);
router.put('/price-charts/:id', adminController.updatePriceChart);
router.delete('/price-charts/:id', adminController.deletePriceChart);

// ==================== WALLET MANAGEMENT ====================
router.get('/wallet/bloggers', adminController.getBloggersWallets);
router.get('/wallet/payment-history', adminController.getPaymentHistory);
router.get('/wallet/withdrawal-requests', adminController.getWithdrawalRequests);
router.get('/wallet/withdrawal-requests/:id', adminController.getWithdrawalRequestDetail);
router.put('/wallet/withdrawal-requests/:id/approve', adminController.approveWithdrawal);
router.put('/wallet/withdrawal-requests/:id/reject', adminController.rejectWithdrawal);

// ==================== CREATE ACCOUNT FROM SITES ====================
router.get('/sites/pending-accounts', adminController.getSitesForAccountCreation);
router.post('/sites/create-accounts', adminController.createAccountsFromSites);

// ==================== PENDING BULK REQUESTS ====================
router.get('/sites/pending-bulk', adminController.getPendingBulkRequests);
router.get('/sites/pending-bulk/:id/download', adminController.downloadBulkFile);
router.put('/sites/pending-bulk/:id/accept', adminController.acceptBulkRequest);
router.put('/sites/pending-bulk/:id/reject', adminController.rejectBulkRequest);

// ==================== SITES LIST (View All Sites) ====================
router.get('/sites/list', adminController.getWebsitesList);

// ==================== CAREERS MANAGEMENT ====================
router.get('/careers', adminController.getCareers);
router.post('/careers', adminController.createCareer);
router.get('/careers/:id', adminController.getCareerById);
router.put('/careers/:id', adminController.updateCareer);
router.delete('/careers/:id', adminController.deleteCareer);

// ==================== FAQs MANAGEMENT ====================
router.get('/faqs', adminController.getFaqs);
router.post('/faqs', adminController.createFaq);
router.get('/faqs/:id', adminController.getFaqById);
router.put('/faqs/:id', adminController.updateFaq);
router.delete('/faqs/:id', adminController.deleteFaq);

// ==================== VIDEOS MANAGEMENT ====================
router.get('/videos', adminController.getVideos);
router.post('/videos', adminController.createVideo);
router.get('/videos/:id', adminController.getVideoById);
router.put('/videos/:id', adminController.updateVideo);
router.delete('/videos/:id', adminController.deleteVideo);

// ==================== COUNTRIES MANAGEMENT ====================
router.get('/countries', adminController.getCountries);
router.post('/countries', adminController.createCountry);
router.get('/countries/:id', adminController.getCountryById);
router.put('/countries/:id', adminController.updateCountry);
router.delete('/countries/:id', adminController.deleteCountry);

module.exports = router;

