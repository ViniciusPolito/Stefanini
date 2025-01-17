package com.spring.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.spring.model.FileDB;
import com.spring.repository.FileDBRepository;

@Service
public class FileStorageService {

	private final Path root = Paths.get("src/uploads");
	private final Path down = Paths.get("src/downloads");

	// Banco de dados
	//////////
	@Autowired
	private FileDBRepository fileDBRepository;

	// Salva o arquivo e seus dados no DB
	public FileDB store(MultipartFile file) throws IOException {
		String fileName = StringUtils.cleanPath(file.getOriginalFilename());
		FileDB FileDB = new FileDB(fileName, root.toString(),down.toString());

		return fileDBRepository.save(FileDB);
	}

	// Retorna id do arquivo
	public FileDB getFile(String id) {
		return fileDBRepository.findById(id).get();
	}

	// Retorna todos os arquivos no DB
	public Stream<FileDB> getAllFiles() {
		return fileDBRepository.findAll().stream();
	}

	//////////
	// Banco de dados

	public void init() {
		try {
			Files.createDirectory(root);
			Files.createDirectory(down);
		} catch (IOException e) {
			throw new RuntimeException("Could not initialize folder for upload!");
		}
	}

	public void save(MultipartFile file) {
		try {
			Files.copy(file.getInputStream(), this.root.resolve(file.getOriginalFilename()));
		} catch (Exception e) {
			System.out.println(e);
			throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
		}
	}

	public Resource load(String filename) {
		try {
			Path file = down.resolve(filename);
			// Path file = root.resolve(filename);
			Resource resource = new UrlResource(file.toUri());

			if (resource.exists() || resource.isReadable()) {
				return resource;
			} else {
				throw new RuntimeException("Could not read the file!");
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException("Error: " + e.getMessage());
		}
	}

	public void deleteAll() {
		FileSystemUtils.deleteRecursively(root.toFile());
		FileSystemUtils.deleteRecursively(down.toFile());
	}

	public Stream<Path> loadAll() {
		try {
			return Files.walk(this.root, 1).filter(path -> !path.equals(this.root)).map(this.root::relativize);
		} catch (IOException e) {
			throw new RuntimeException("Could not load the files!");
		}
	}

	public void transform(String filename) {

		// Inicia o OpenCV
		nu.pattern.OpenCV.loadLocally();

		// Leitura do arquivo do banco
		String imgFile = "./src/uploads/" + filename;

		// Vai ler a imagem e colocar em grayscale 0/Preto-255/Branco
		Mat src = Imgcodecs.imread(imgFile, 0);

		// Binarização Inicio
		int width = (int) (0.2 * (src.width()));
		int height = (int) (0.3 * (src.height()));
		int x = (int) (0.8 * (src.width()));
		int y = 0;

		Rect rectCrop = new Rect(x, y, width, height);
		Mat output_rec = src.submat(rectCrop);
		// Binarização Fim

		// Threshold Inicio
		int heightMat = output_rec.height(); // Altura da imagem recortada
		int widthMath = output_rec.width(); // Largura da imagem recortada

		// Modifico cada pixel, verificando se o valor dele está acima ou abaixo do
		// limiar, então transformo em preto ou branco.
		for (int i = 0; i < heightMat; i++) {
			for (int s = 0; s < widthMath; s++) {

				double pixel[] = output_rec.get(i, s);
				if (pixel[0] >= 165) {
					output_rec.put(i, s, 255);
				} else {
					output_rec.put(i, s, 0);
				}

			}
		}
		// Threshold Fim

		// Escrita do arquivo no banco
		Imgcodecs.imwrite("./src/downloads/" + filename, output_rec);
	}
}