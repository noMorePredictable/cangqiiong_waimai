package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /*
    * 用户下单
    * */
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //处理异常（空数据）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook==null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //查询购物车数据
        Long currentId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(currentId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        //插入数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setUserId(currentId);
        orderMapper.insert(orders);

        //清空数据

        //封装vo返回结果


        return null;

        /*
         * 订单支付
         *
         * @param ordersPaymentDTO
         * @return
         */
        public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
            // 当前登录用户id
            Long userId = BaseContext.getCurrentId();
            User user = userMapper.getById(userId);

            //调用微信支付接口，生成预支付交易单
            JSONObject jsonObject = weChatPayUtil.pay(
                    ordersPaymentDTO.getOrderNumber(), //商户订单号
                    new BigDecimal(0.01), //支付金额，单位 元
                    "苍穹外卖订单", //商品描述
                    user.getOpenid() //微信用户的openid
            );

            if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
                throw new OrderBusinessException("该订单已支付");
            }

            OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
            vo.setPackageStr(jsonObject.getString("package"));

            return vo;
        }

        /**
         * 支付成功，修改订单状态
         *
         * @param outTradeNo
         */
        public void paySuccess(String outTradeNo) {

            // 根据订单号查询订单
            Orders ordersDB = orderMapper.getByNumber(outTradeNo);

            // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
            Orders orders = Orders.builder()
                    .id(ordersDB.getId())
                    .status(Orders.TO_BE_CONFIRMED)
                    .payStatus(Orders.PAID)
                    .checkoutTime(LocalDateTime.now())
                    .build();

            orderMapper.update(orders);
        }
    }
}
