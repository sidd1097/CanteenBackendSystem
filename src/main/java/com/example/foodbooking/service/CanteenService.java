package com.example.foodbooking.service;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.foodbooking.customexception.ServerSideException;
import com.example.foodbooking.dto.ApiResponse;
import com.example.foodbooking.entity.Orders;
import com.example.foodbooking.entity.Status;
import com.example.foodbooking.repository.OrdersRepository;
import com.example.foodbooking.utils.QRCodeGenerator;

@Service
@Transactional
public class CanteenService {

	@Autowired
	private OrdersRepository ordersRepository;

	public ApiResponse servedOrder(Long orderId) throws ServerSideException {
		Orders order = ordersRepository.findById(orderId)
				.orElseThrow(() -> new ServerSideException("Order with such Order Id doesn't exists"));
		order.setStatus(Status.SERVED);
		QRCodeGenerator.deleteQRCode(order);
		return new ApiResponse("Order Served Successfully");
	}

}
