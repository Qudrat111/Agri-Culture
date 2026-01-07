// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

/**
 * @title ProcurementOrder
 * @dev Smart contract for agricultural procurement with escrow functionality
 * Supports order creation, delivery confirmation, and cancellation/refund
 */
contract ProcurementOrder {
    
    enum OrderStatus { 
        Created, 
        Confirmed, 
        Delivered, 
        Cancelled, 
        Refunded 
    }
    
    struct Order {
        address buyer;
        address seller;
        uint256 amount;
        OrderStatus status;
        uint256 createdAt;
        uint256 updatedAt;
        bool exists;
    }
    
    mapping(bytes32 => Order) public orders;
    
    event OrderCreated(
        bytes32 indexed orderId,
        address indexed buyer,
        address indexed seller,
        uint256 amount,
        uint256 timestamp
    );
    
    event OrderConfirmed(
        bytes32 indexed orderId,
        uint256 timestamp
    );
    
    event OrderDelivered(
        bytes32 indexed orderId,
        uint256 timestamp
    );
    
    event OrderCancelled(
        bytes32 indexed orderId,
        uint256 timestamp
    );
    
    event OrderRefunded(
        bytes32 indexed orderId,
        uint256 amount,
        uint256 timestamp
    );
    
    modifier orderExists(bytes32 orderId) {
        require(orders[orderId].exists, "Order does not exist");
        _;
    }
    
    modifier onlyBuyer(bytes32 orderId) {
        require(msg.sender == orders[orderId].buyer, "Only buyer can call this");
        _;
    }
    
    modifier onlySeller(bytes32 orderId) {
        require(msg.sender == orders[orderId].seller, "Only seller can call this");
        _;
    }
    
    modifier inStatus(bytes32 orderId, OrderStatus expectedStatus) {
        require(orders[orderId].status == expectedStatus, "Invalid order status");
        _;
    }
    
    /**
     * @dev Create a new procurement order with escrow
     * @param orderId Unique identifier for the order
     * @param seller Address of the seller
     */
    function createOrder(bytes32 orderId, address seller) 
        external 
        payable 
    {
        require(!orders[orderId].exists, "Order already exists");
        require(seller != address(0), "Invalid seller address");
        require(msg.value > 0, "Amount must be greater than 0");
        require(msg.sender != seller, "Buyer and seller cannot be the same");
        
        orders[orderId] = Order({
            buyer: msg.sender,
            seller: seller,
            amount: msg.value,
            status: OrderStatus.Created,
            createdAt: block.timestamp,
            updatedAt: block.timestamp,
            exists: true
        });
        
        emit OrderCreated(orderId, msg.sender, seller, msg.value, block.timestamp);
    }
    
    /**
     * @dev Confirm order acceptance by seller
     * @param orderId The order to confirm
     */
    function confirmOrder(bytes32 orderId) 
        external 
        orderExists(orderId)
        onlySeller(orderId)
        inStatus(orderId, OrderStatus.Created)
    {
        orders[orderId].status = OrderStatus.Confirmed;
        orders[orderId].updatedAt = block.timestamp;
        
        emit OrderConfirmed(orderId, block.timestamp);
    }
    
    /**
     * @dev Confirm delivery and release funds to seller
     * @param orderId The order to mark as delivered
     */
    function confirmDelivery(bytes32 orderId) 
        external 
        orderExists(orderId)
        onlyBuyer(orderId)
        inStatus(orderId, OrderStatus.Confirmed)
    {
        Order storage order = orders[orderId];
        order.status = OrderStatus.Delivered;
        order.updatedAt = block.timestamp;
        
        // Transfer funds to seller
        (bool success, ) = payable(order.seller).call{value: order.amount}("");
        require(success, "Transfer to seller failed");
        
        emit OrderDelivered(orderId, block.timestamp);
    }
    
    /**
     * @dev Cancel order before confirmation
     * @param orderId The order to cancel
     */
    function cancelOrder(bytes32 orderId) 
        external 
        orderExists(orderId)
        onlyBuyer(orderId)
        inStatus(orderId, OrderStatus.Created)
    {
        orders[orderId].status = OrderStatus.Cancelled;
        orders[orderId].updatedAt = block.timestamp;
        
        emit OrderCancelled(orderId, block.timestamp);
        
        // Refund buyer
        _refund(orderId);
    }
    
    /**
     * @dev Request refund for confirmed but undelivered orders
     * Can be called by buyer after a timeout period
     * @param orderId The order to refund
     */
    function requestRefund(bytes32 orderId) 
        external 
        orderExists(orderId)
        onlyBuyer(orderId)
    {
        Order storage order = orders[orderId];
        require(
            order.status == OrderStatus.Created || order.status == OrderStatus.Confirmed,
            "Order cannot be refunded"
        );
        
        // In production, add timeout logic here
        // require(block.timestamp > order.createdAt + 30 days, "Refund period not reached");
        
        order.status = OrderStatus.Cancelled;
        order.updatedAt = block.timestamp;
        
        _refund(orderId);
    }
    
    /**
     * @dev Internal function to process refunds
     * @param orderId The order to refund
     */
    function _refund(bytes32 orderId) private {
        Order storage order = orders[orderId];
        uint256 refundAmount = order.amount;
        order.amount = 0; // Prevent reentrancy
        
        (bool success, ) = payable(order.buyer).call{value: refundAmount}("");
        require(success, "Refund transfer failed");
        
        emit OrderRefunded(orderId, refundAmount, block.timestamp);
    }
    
    /**
     * @dev Get order details
     * @param orderId The order to query
     */
    function getOrder(bytes32 orderId) 
        external 
        view 
        orderExists(orderId)
        returns (
            address buyer,
            address seller,
            uint256 amount,
            OrderStatus status,
            uint256 createdAt,
            uint256 updatedAt
        ) 
    {
        Order memory order = orders[orderId];
        return (
            order.buyer,
            order.seller,
            order.amount,
            order.status,
            order.createdAt,
            order.updatedAt
        );
    }
    
    /**
     * @dev Check if order exists
     * @param orderId The order to check
     */
    function doesOrderExist(bytes32 orderId) external view returns (bool) {
        return orders[orderId].exists;
    }
}
