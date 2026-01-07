const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("ProcurementOrder", function () {
  let procurementOrder;
  let owner, buyer, seller, otherAccount;
  const orderAmount = ethers.parseEther("1.0");

  beforeEach(async function () {
    [owner, buyer, seller, otherAccount] = await ethers.getSigners();
    
    const ProcurementOrder = await ethers.getContractFactory("ProcurementOrder");
    procurementOrder = await ProcurementOrder.deploy();
  });

  describe("Order Creation", function () {
    it("Should create a new order successfully", async function () {
      const orderId = ethers.id("order-001");
      
      await expect(
        procurementOrder.connect(buyer).createOrder(orderId, seller.address, {
          value: orderAmount
        })
      )
        .to.emit(procurementOrder, "OrderCreated")
        .withArgs(orderId, buyer.address, seller.address, orderAmount, await getBlockTimestamp());
      
      const order = await procurementOrder.getOrder(orderId);
      expect(order.buyer).to.equal(buyer.address);
      expect(order.seller).to.equal(seller.address);
      expect(order.amount).to.equal(orderAmount);
      expect(order.status).to.equal(0); // Created status
    });

    it("Should fail to create order with zero amount", async function () {
      const orderId = ethers.id("order-002");
      
      await expect(
        procurementOrder.connect(buyer).createOrder(orderId, seller.address, {
          value: 0
        })
      ).to.be.revertedWith("Amount must be greater than 0");
    });

    it("Should fail to create order with invalid seller", async function () {
      const orderId = ethers.id("order-003");
      
      await expect(
        procurementOrder.connect(buyer).createOrder(orderId, ethers.ZeroAddress, {
          value: orderAmount
        })
      ).to.be.revertedWith("Invalid seller address");
    });

    it("Should fail to create duplicate order", async function () {
      const orderId = ethers.id("order-004");
      
      await procurementOrder.connect(buyer).createOrder(orderId, seller.address, {
        value: orderAmount
      });
      
      await expect(
        procurementOrder.connect(buyer).createOrder(orderId, seller.address, {
          value: orderAmount
        })
      ).to.be.revertedWith("Order already exists");
    });

    it("Should fail if buyer and seller are the same", async function () {
      const orderId = ethers.id("order-005");
      
      await expect(
        procurementOrder.connect(buyer).createOrder(orderId, buyer.address, {
          value: orderAmount
        })
      ).to.be.revertedWith("Buyer and seller cannot be the same");
    });
  });

  describe("Order Confirmation", function () {
    let orderId;

    beforeEach(async function () {
      orderId = ethers.id("order-confirm-001");
      await procurementOrder.connect(buyer).createOrder(orderId, seller.address, {
        value: orderAmount
      });
    });

    it("Should allow seller to confirm order", async function () {
      await expect(procurementOrder.connect(seller).confirmOrder(orderId))
        .to.emit(procurementOrder, "OrderConfirmed")
        .withArgs(orderId, await getBlockTimestamp());
      
      const order = await procurementOrder.getOrder(orderId);
      expect(order.status).to.equal(1); // Confirmed status
    });

    it("Should fail if non-seller tries to confirm", async function () {
      await expect(
        procurementOrder.connect(otherAccount).confirmOrder(orderId)
      ).to.be.revertedWith("Only seller can call this");
    });

    it("Should fail to confirm already confirmed order", async function () {
      await procurementOrder.connect(seller).confirmOrder(orderId);
      
      await expect(
        procurementOrder.connect(seller).confirmOrder(orderId)
      ).to.be.revertedWith("Invalid order status");
    });
  });

  describe("Delivery Confirmation", function () {
    let orderId;

    beforeEach(async function () {
      orderId = ethers.id("order-delivery-001");
      await procurementOrder.connect(buyer).createOrder(orderId, seller.address, {
        value: orderAmount
      });
      await procurementOrder.connect(seller).confirmOrder(orderId);
    });

    it("Should confirm delivery and transfer funds to seller", async function () {
      const sellerBalanceBefore = await ethers.provider.getBalance(seller.address);
      
      await expect(procurementOrder.connect(buyer).confirmDelivery(orderId))
        .to.emit(procurementOrder, "OrderDelivered")
        .withArgs(orderId, await getBlockTimestamp());
      
      const sellerBalanceAfter = await ethers.provider.getBalance(seller.address);
      expect(sellerBalanceAfter - sellerBalanceBefore).to.equal(orderAmount);
      
      const order = await procurementOrder.getOrder(orderId);
      expect(order.status).to.equal(2); // Delivered status
    });

    it("Should fail if non-buyer tries to confirm delivery", async function () {
      await expect(
        procurementOrder.connect(otherAccount).confirmDelivery(orderId)
      ).to.be.revertedWith("Only buyer can call this");
    });

    it("Should fail to confirm delivery for unconfirmed order", async function () {
      const newOrderId = ethers.id("order-delivery-002");
      await procurementOrder.connect(buyer).createOrder(newOrderId, seller.address, {
        value: orderAmount
      });
      
      await expect(
        procurementOrder.connect(buyer).confirmDelivery(newOrderId)
      ).to.be.revertedWith("Invalid order status");
    });
  });

  describe("Order Cancellation", function () {
    let orderId;

    beforeEach(async function () {
      orderId = ethers.id("order-cancel-001");
      await procurementOrder.connect(buyer).createOrder(orderId, seller.address, {
        value: orderAmount
      });
    });

    it("Should cancel order and refund buyer", async function () {
      const buyerBalanceBefore = await ethers.provider.getBalance(buyer.address);
      
      const tx = await procurementOrder.connect(buyer).cancelOrder(orderId);
      const receipt = await tx.wait();
      const gasUsed = receipt.gasUsed * receipt.gasPrice;
      
      await expect(tx)
        .to.emit(procurementOrder, "OrderCancelled")
        .withArgs(orderId, await getBlockTimestamp());
      
      await expect(tx)
        .to.emit(procurementOrder, "OrderRefunded")
        .withArgs(orderId, orderAmount, await getBlockTimestamp());
      
      const buyerBalanceAfter = await ethers.provider.getBalance(buyer.address);
      expect(buyerBalanceAfter - buyerBalanceBefore + gasUsed).to.equal(orderAmount);
      
      const order = await procurementOrder.getOrder(orderId);
      expect(order.status).to.equal(3); // Cancelled status
      expect(order.amount).to.equal(0); // Amount should be 0 after refund
    });

    it("Should fail if non-buyer tries to cancel", async function () {
      await expect(
        procurementOrder.connect(seller).cancelOrder(orderId)
      ).to.be.revertedWith("Only buyer can call this");
    });

    it("Should fail to cancel already confirmed order", async function () {
      await procurementOrder.connect(seller).confirmOrder(orderId);
      
      await expect(
        procurementOrder.connect(buyer).cancelOrder(orderId)
      ).to.be.revertedWith("Invalid order status");
    });
  });

  describe("Refund Request", function () {
    let orderId;

    beforeEach(async function () {
      orderId = ethers.id("order-refund-001");
      await procurementOrder.connect(buyer).createOrder(orderId, seller.address, {
        value: orderAmount
      });
    });

    it("Should allow buyer to request refund for created order", async function () {
      const buyerBalanceBefore = await ethers.provider.getBalance(buyer.address);
      
      const tx = await procurementOrder.connect(buyer).requestRefund(orderId);
      const receipt = await tx.wait();
      const gasUsed = receipt.gasUsed * receipt.gasPrice;
      
      const buyerBalanceAfter = await ethers.provider.getBalance(buyer.address);
      expect(buyerBalanceAfter - buyerBalanceBefore + gasUsed).to.equal(orderAmount);
      
      const order = await procurementOrder.getOrder(orderId);
      expect(order.status).to.equal(3); // Cancelled status
    });

    it("Should allow buyer to request refund for confirmed order", async function () {
      await procurementOrder.connect(seller).confirmOrder(orderId);
      
      const buyerBalanceBefore = await ethers.provider.getBalance(buyer.address);
      
      const tx = await procurementOrder.connect(buyer).requestRefund(orderId);
      const receipt = await tx.wait();
      const gasUsed = receipt.gasUsed * receipt.gasPrice;
      
      await expect(tx)
        .to.emit(procurementOrder, "OrderRefunded");
      
      const buyerBalanceAfter = await ethers.provider.getBalance(buyer.address);
      expect(buyerBalanceAfter - buyerBalanceBefore + gasUsed).to.equal(orderAmount);
    });

    it("Should fail to refund delivered order", async function () {
      await procurementOrder.connect(seller).confirmOrder(orderId);
      await procurementOrder.connect(buyer).confirmDelivery(orderId);
      
      await expect(
        procurementOrder.connect(buyer).requestRefund(orderId)
      ).to.be.revertedWith("Order cannot be refunded");
    });

    it("Should fail if non-buyer requests refund", async function () {
      await expect(
        procurementOrder.connect(seller).requestRefund(orderId)
      ).to.be.revertedWith("Only buyer can call this");
    });
  });

  describe("Order Queries", function () {
    it("Should return order details correctly", async function () {
      const orderId = ethers.id("order-query-001");
      await procurementOrder.connect(buyer).createOrder(orderId, seller.address, {
        value: orderAmount
      });
      
      const order = await procurementOrder.getOrder(orderId);
      expect(order.buyer).to.equal(buyer.address);
      expect(order.seller).to.equal(seller.address);
      expect(order.amount).to.equal(orderAmount);
      expect(order.status).to.equal(0);
    });

    it("Should check if order exists", async function () {
      const orderId = ethers.id("order-exists-001");
      
      expect(await procurementOrder.doesOrderExist(orderId)).to.be.false;
      
      await procurementOrder.connect(buyer).createOrder(orderId, seller.address, {
        value: orderAmount
      });
      
      expect(await procurementOrder.doesOrderExist(orderId)).to.be.true;
    });

    it("Should fail to get non-existent order", async function () {
      const orderId = ethers.id("order-nonexistent");
      
      await expect(
        procurementOrder.getOrder(orderId)
      ).to.be.revertedWith("Order does not exist");
    });
  });

  // Helper function to get current block timestamp
  async function getBlockTimestamp() {
    const blockNumber = await ethers.provider.getBlockNumber();
    const block = await ethers.provider.getBlock(blockNumber);
    return block.timestamp;
  }
});
