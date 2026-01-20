require('dotenv').config();
const express = require('express');
const cors = require('cors');
const http = require('http');
const path = require('path');
const { Server } = require('socket.io');
const db = require('./config/database');
const errorHandler = require('./middleware/errorHandler');
const logger = require('./utils/logger');

// Import routes
const authRoutes = require('./routes/auth');
const adminRoutes = require('./routes/admin');
const teamRoutes = require('./routes/team');
const managerRoutes = require('./routes/manager');
const writerRoutes = require('./routes/writer');
const bloggerRoutes = require('./routes/blogger');
const accountantRoutes = require('./routes/accountant');
const configRoutes = require('./routes/config');
const threadRoutes = require('./routes/threads');

const app = express();
const PORT = process.env.PORT || 5001;

// Create HTTP server and attach Socket.io
const server = http.createServer(app);

// CORS Configuration - allow multiple origins for development and Cloudflare tunnel
const allowedOrigins = [
  'http://localhost:5173',
  'http://localhost:5174',
  'http://localhost:3000',
  'https://installation-policies-nose-julian.trycloudflare.com'
];
console.log(`🔒 CORS configured for origins: ${allowedOrigins.join(', ')}`);

// Initialize Socket.io with CORS
const io = new Server(server, {
  cors: {
    origin: function (origin, callback) {
      if (!origin) return callback(null, true);
      if (allowedOrigins.indexOf(origin) !== -1) {
        return callback(null, true);
      }
      if (origin && origin.endsWith('.trycloudflare.com')) {
        return callback(null, true);
      }
      return callback(new Error('Not allowed by CORS'));
    },
    methods: ['GET', 'POST'],
    credentials: true
  }
});

// Make io accessible to routes/controllers
app.set('io', io);

// Socket.io connection handling
io.on('connection', (socket) => {
  console.log(`🔌 Client connected: ${socket.id}`);

  // Join room for specific order updates
  socket.on('join-order', (orderId) => {
    socket.join(`order-${orderId}`);
    console.log(`📦 Socket ${socket.id} joined order-${orderId}`);
  });

  // Leave order room
  socket.on('leave-order', (orderId) => {
    socket.leave(`order-${orderId}`);
    console.log(`📦 Socket ${socket.id} left order-${orderId}`);
  });

  // Join room for orders list updates
  socket.on('join-orders-list', () => {
    socket.join('orders-list');
    console.log(`📋 Socket ${socket.id} joined orders-list`);
  });

  socket.on('disconnect', () => {
    console.log(`🔌 Client disconnected: ${socket.id}`);
  });
});

// Export io for use in controllers
module.exports.io = io;

app.use(cors({
  origin: function (origin, callback) {
    // Allow requests with no origin (like mobile apps or curl)
    if (!origin) return callback(null, true);
    if (allowedOrigins.indexOf(origin) !== -1) {
      return callback(null, true);
    }
    // Allow any trycloudflare.com subdomain for tunnel testing
    if (origin && origin.endsWith('.trycloudflare.com')) {
      return callback(null, true);
    }
    return callback(new Error('Not allowed by CORS'));
  },
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Serve static files from uploads directory
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// Request logging middleware - logs ALL incoming requests
app.use((req, res, next) => {
  const startTime = Date.now();

  // Log on request completion
  res.on('finish', () => {
    const duration = Date.now() - startTime;
    const statusColor = res.statusCode >= 400 ? '\x1b[31m' : '\x1b[32m';
    console.log(`${statusColor}[${res.statusCode}]\x1b[0m ${req.method} ${req.originalUrl} - ${duration}ms`);
  });

  next();
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.status(200).json({
    status: 'OK',
    message: 'Workflow Management API is running',
    timestamp: new Date().toISOString(),
    port: PORT,
    corsOrigins: allowedOrigins,
    socketEnabled: true
  });
});

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/team', teamRoutes);
app.use('/api/manager', managerRoutes);
app.use('/api/writer', writerRoutes);
app.use('/api/blogger', bloggerRoutes);
app.use('/api/accountant', accountantRoutes);
app.use('/api/config', configRoutes);
app.use('/api/threads', threadRoutes);

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    error: 'Not Found',
    message: `Cannot ${req.method} ${req.path}`
  });
});

// Error handling middleware (must be last)
app.use(errorHandler);

// Start server with Socket.io
server.listen(PORT, () => {
  console.log(`🚀 Server is running on port ${PORT}`);
  console.log(`🔌 Socket.io enabled for real-time updates`);
  console.log(`📊 Environment: ${process.env.NODE_ENV || 'development'}`);
  console.log(`🔗 Health check: http://localhost:${PORT}/health`);
});

module.exports = app;
