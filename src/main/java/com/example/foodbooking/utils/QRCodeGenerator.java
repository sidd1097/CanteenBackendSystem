package com.example.foodbooking.utils;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.example.foodbooking.entity.Orders;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRCodeGenerator {

	public static String getPath(Orders order) {
		String qrCodePath = "C:/Users/dac/Downloads/Java/OrderQRCodes/" + order.getStudent().getId() + "/";
		String qrCodeName = qrCodePath + order.getStudent().getPrn() + "_" + order.getId() + "- QRCODE.png";
		return qrCodeName;
	}

	public static void generateQRCode(Orders order, Long total) throws WriterException, IOException {
		String qrCodeName = getPath(order);
		var qrCodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = qrCodeWriter.encode(
				"ID: " + order.getId() + "\n" + "Name: " + order.getStudent().getFirst_name() + " "
						+ order.getStudent().getLast_name() + "\n" + "Dish: " + order.getDish().getName() + "\n"
						+ "Quantity: " + order.getQuantity() + "\n" + "Total Amount: " + total + "\n" + "Status: "
						+ order.getStatus() + "\n" + "Placed at: " + order.getTimestamp() + "\n",
				BarcodeFormat.QR_CODE, 700, 700);

		Path path = Paths.get(qrCodeName);

		// Ensure the directory exists
		Path parentDir = path.getParent();
		if (parentDir != null) {
			java.nio.file.Files.createDirectories(parentDir);
		}
		MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
	}

	public static void deleteQRCode(Orders order) {
		String qrCodeName = getPath(order);
		Path path = Paths.get(qrCodeName);
		try {
			Files.delete(path);
			System.out.println("File deleted successfully.");
		} catch (NoSuchFileException e) {
			System.err.println("No such file or directory: " + e.getMessage());
		} catch (DirectoryNotEmptyException e) {
			System.err.println("Directory is not empty: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("IOException: " + e.getMessage());
		}
	}
}
