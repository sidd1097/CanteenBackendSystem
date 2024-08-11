package com.example.foodbooking.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.foodbooking.customexception.ServerSideException;
import com.example.foodbooking.dto.ApiResponse;
import com.example.foodbooking.dto.DishDTO;
import com.example.foodbooking.dto.FriendDTO;
import com.example.foodbooking.dto.OrdersDTO;
import com.example.foodbooking.entity.Dish;
import com.example.foodbooking.entity.Orders;
import com.example.foodbooking.entity.Receipt;
import com.example.foodbooking.entity.Status;
import com.example.foodbooking.entity.Student;
import com.example.foodbooking.repository.DishRepository;
import com.example.foodbooking.repository.OrdersRepository;
import com.example.foodbooking.repository.ReceiptRepository;
import com.example.foodbooking.repository.StudentRepository;
import com.example.foodbooking.security.CustomUserDetails;
import com.example.foodbooking.utils.QRCodeGenerator;
import com.google.zxing.WriterException;

@Transactional
@Service
public class StudentService {

	@Autowired
	private ModelMapper mapper;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private DishRepository dishReposistory;

	@Autowired
	private ReceiptRepository receiptRepository;

	@Autowired
	private OrdersRepository ordersRepository;

	public Long processUserDetails() {
		CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication()
				.getPrincipal();
		return Long.parseLong(principal.getId());
	}

//	public Boolean login(StudentLoginDTO studentLoginDTO) throws ServerSideException {
//		Student student = studentRepository.findByUsername(studentLoginDTO.getUsername())
//				.orElseThrow(() -> new ServerSideException("Invalid Email or Password"));
//		System.out.println(student.getFirst_name());
//		return true;
//	}

	public List<DishDTO> getAllDishes() {
		return dishReposistory.findAll().stream().filter(dish -> dish.getQuantity_remaining() > 0)
				.map(dish -> mapper.map(dish, DishDTO.class))
				.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
	}

	public ApiResponse placeOrder(OrdersDTO order, Long dishid)
			throws ServerSideException, WriterException, IOException {
		Long id = processUserDetails();
		System.out.println(id);
		Orders placeNewOrder = mapper.map(order, Orders.class);
		placeNewOrder.setStatus(Status.PLACED);
		Student student = studentRepository.findById(id)
				.orElseThrow(() -> new ServerSideException("No such Student exists with given UserId"));
		Dish dish = dishReposistory.findById(dishid)
				.orElseThrow(() -> new ServerSideException("No such Dish exists with given DishId"));
		Long totalAmount = (long) (dish.getPrice() * placeNewOrder.getQuantity());
		if (dish.getQuantity_remaining() <= 0)
			return new ApiResponse("Dish is not available Right Now");
		if (placeNewOrder.getQuantity() > dish.getQuantity_remaining())
			return new ApiResponse("Your Order quantity exceeds remaining quantity");
		if (totalAmount > student.getTokens_balance())
			return new ApiResponse("Not Enough Balance Available");

		dish.setQuantity_remaining(dish.getQuantity_remaining() - placeNewOrder.getQuantity());
		student.setTokens_balance(student.getTokens_balance() - totalAmount);

		student.addOrders(placeNewOrder);
		dish.addOrder(placeNewOrder);

		Receipt receipt = new Receipt(placeNewOrder);
		receipt.setTotal(totalAmount);
		receiptRepository.save(receipt);
		placeNewOrder.setReceipt(receipt);

		placeNewOrder = ordersRepository.save(placeNewOrder);
		placeNewOrder.setQrcodepath(QRCodeGenerator.getPath(placeNewOrder));
		System.out.println("Order ID: " + placeNewOrder.getId() + "\n" + "Time: " + placeNewOrder.getTimestamp());
		QRCodeGenerator.generateQRCode(placeNewOrder, receipt.getTotal());

		return new ApiResponse("Order Placed Successfully");
	}

	public Object getAllOrders() throws ServerSideException {
		Long studentId = processUserDetails();
		Student student = studentRepository.findById(studentId)
				.orElseThrow(() -> new ServerSideException("No such Student exists with given UserId"));
		List<Orders> orders = ordersRepository.findOrdersByStudent(student);
		List<OrdersDTO> ordersDTO = new ArrayList<OrdersDTO>();
		orders.stream().filter(values -> !values.getStatus().equals(Status.SERVED)).forEach(values -> {
			OrdersDTO addOrderDto = mapper.map(values, OrdersDTO.class);
			addOrderDto.setDish(values.getDish().getName());
			ordersDTO.add(addOrderDto);
		});
		if (ordersDTO.size() > 0)
			return ordersDTO;
		else
			return new ApiResponse("Currently No Orders Are Placed !!!");
	}

	public ApiResponse deleteParticularOrder(Long orderId) throws ServerSideException {
		Long studentId = processUserDetails();
		Orders orders = ordersRepository.findById(orderId)
				.orElseThrow(() -> new ServerSideException("No such Order exists with given OrderId"));
		orders.set_cancelled(true);
		orders.setStatus(Status.CANCELLED);
		Student student = studentRepository.findById(studentId)
				.orElseThrow(() -> new ServerSideException("No such Student exists with given UserId"));
		Dish dish = dishReposistory.findById(orders.getDish().getId())
				.orElseThrow(() -> new ServerSideException("No such Dish exists with given DishId"));
		QRCodeGenerator.deleteQRCode(orders);
		student.deleteOrder(orders);
		dish.removeOrder(orders);
		dish.setQuantity_remaining(dish.getQuantity_remaining() + orders.getQuantity());
		student.setTokens_balance(student.getTokens_balance() + orders.getReceipt().getTotal());
		return new ApiResponse("Order Deleted Successfully");
	}

	public List<FriendDTO> getAllFriends() throws ServerSideException {
		Student student = studentRepository.findById(processUserDetails())
				.orElseThrow(() -> new ServerSideException("No such Student exists with given UserId"));
		return student.getFriends().stream().map(values -> mapper.map(values, FriendDTO.class)).collect(ArrayList::new,
				ArrayList::add, ArrayList::addAll);
	}

	public ApiResponse addNewFriend(String prn) throws ServerSideException {
		Student student = studentRepository.findById(processUserDetails())
				.orElseThrow(() -> new ServerSideException("No such Student exists with given UserId"));
		Student friend = studentRepository.findByPrn(prn)
				.orElseThrow(() -> new ServerSideException("No such Student exists with given Prn"));
		if (student.getId().equals(friend.getId()))
			return new ApiResponse("Sorry You Can't be your own friend");
		if (student.isFriend(prn)) {
			return new ApiResponse("Given Prn is already a friend");
		}
		student.addFriend(friend);
		return new ApiResponse("Friend Added Successfully");
	}

	public ApiResponse deleteFriend(Long friendId) throws ServerSideException {
		Student student = studentRepository.findById(processUserDetails())
				.orElseThrow(() -> new ServerSideException("No such Student exists with given UserId"));
		Student friend = studentRepository.findById(friendId)
				.orElseThrow(() -> new ServerSideException("No such Student exists with given UserId"));
		student.removeFriend(friend);
		return new ApiResponse("Friend Deleted Successfully");
	}

	public ApiResponse delegateOrder(Long orderId, Long friendId)
			throws ServerSideException, WriterException, IOException {
		Student student = studentRepository.findById(processUserDetails())
				.orElseThrow(() -> new ServerSideException("No such Student exists with given UserId"));
		Student friend = studentRepository.findById(friendId)
				.orElseThrow(() -> new ServerSideException("No such Friend exists with given UserId"));
		Orders order = ordersRepository.findById(orderId)
				.orElseThrow(() -> new ServerSideException("No such Order exists with given OrderId"));
		QRCodeGenerator.deleteQRCode(order);
		student.deleteOrder(order);
		friend.addOrders(order);
		Receipt receipt = order.getReceipt();
		order.setQrcodepath(QRCodeGenerator.getPath(order));
		QRCodeGenerator.generateQRCode(order, receipt.getTotal());
		return new ApiResponse("Order Delegated Successfully");
	}

}
