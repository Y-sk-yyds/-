DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `orders`;
-- 创建订单表
CREATE TABLE `orders` (
                          `id` bigint PRIMARY KEY AUTO_INCREMENT,
                          `order_no` varchar(32) NOT NULL UNIQUE,
                          `user_id` bigint NOT NULL,
                          `total_amount` decimal(10,2) NOT NULL,
                          `status` varchar(20) DEFAULT 'PENDING' COMMENT 'PENDING-待支付, PAID-已支付, DELIVERING-配送中, COMPLETED-已完成, CANCELLED-已取消',
                          `address` varchar(255),
                          `remark` varchar(255),
                          `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
                          INDEX idx_user_id (user_id)
);

-- 创建订单详情表
CREATE TABLE `order_item` (
                              `id` bigint PRIMARY KEY AUTO_INCREMENT,
                              `order_id` bigint NOT NULL,
                              `product_id` bigint NOT NULL,
                              `product_name` varchar(100) NOT NULL,
                              `product_price` decimal(10,2) NOT NULL,
                              `quantity` int NOT NULL,
                              `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
                              INDEX idx_order_id (order_id)
);


CREATE TABLE `login_ticket` (
                                `id` bigint PRIMARY KEY AUTO_INCREMENT,
                                `token` varchar(64) NOT NULL UNIQUE,
                                `user_id` bigint DEFAULT NULL,
                                `status` tinyint DEFAULT 0 COMMENT '0未使用,1已确认',
                                `expire_time` datetime NOT NULL,
                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP
);


INSERT INTO product (name, price, description, image_url, status, created_at) VALUES
('经典牛肉汉堡', 28.00, '新鲜牛肉饼搭配生菜、番茄和秘制酱料，口感丰富', '/images/beef_burger.jpg', 1, NOW()),
('意大利肉酱面', 35.00, '传统意式肉酱，搭配Q弹意面，撒上帕玛森奶酪', '/images/spaghetti.jpg', 1, NOW()),
('招牌披萨', 58.00, '薄底披萨，配料丰富，包含培根、蘑菇、彩椒和芝士', '/images/pizza.jpg', 1, NOW()),
('凯撒沙拉', 22.00, '新鲜罗马生菜，搭配鸡胸肉、面包丁和凯撒酱', '/images/caesar_salad.jpg', 1, NOW()),
('日式鳗鱼饭', 45.00, '蒲烧鳗鱼搭配米饭，配味噌汤和渍物', '/images/unagi_don.jpg', 1, NOW()),
('泰式冬阴功汤', 32.00, '酸辣开胃，含有鲜虾、蘑菇、香茅和柠檬叶', '/images/tom_yum.jpg', 1, NOW()),
('法式焦糖布丁', 18.00, '表面焦糖脆壳，口感丝滑香甜', '/images/crème_brûlée.jpg', 1, NOW()),
('抹茶千层蛋糕', 28.00, '层层饼皮叠加抹茶奶油，甜而不腻', '/images/matcha_cake.jpg', 1, NOW()),
('鲜榨橙汁', 15.00, '100%鲜榨橙汁，富含维生素C', '/images/orange_juice.jpg', 1, NOW()),
('美式咖啡', 12.00, '精选咖啡豆现磨，香醇浓郁', '/images/american_coffee.jpg', 1, NOW());