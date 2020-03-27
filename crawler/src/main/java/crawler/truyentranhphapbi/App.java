package crawler.truyentranhphapbi;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.ProfilesIni;
import org.openqa.selenium.remote.DesiredCapabilities;

public class App {

	private String url = "";
	private static List<String> listOfTitles;
	private static List<String> listOfCompleted;

	public static WebDriver driver;

	public static void main(String[] args) throws InterruptedException {
		 System.setProperty("webdriver.gecko.driver","./geckoDriver/geckodriver-v0.25.0-win64/geckodriver.exe");
		 
		 startCrawler(); 
		 doCrawl();

	}

	public static void startCrawler() {
		System.out.println("==========================");
		System.out.println("Start crawling from " + readFromFiles("./url.txt").get(0));
		System.out.println("Time start: " + java.time.LocalDateTime.now());
		System.out.println("==========================");

		listOfTitles = readFromFiles("./title.txt");
		if (listOfTitles == null) {
			System.out.println("The Title file is empty!");
		} else {
			System.out.println("==========================");
			System.out.println("List of Comic to be crawled");
			for (String _s : listOfTitles)
				System.out.println(_s);
			System.out.println("==========================");
		}
	}

	public static void doCrawl() throws InterruptedException {
		String cUrl = "";
		String cTitle = "";

		ProfilesIni profile;
		FirefoxProfile testProfile;
		DesiredCapabilities dc;
		FirefoxOptions opt;

		for (String title : listOfTitles) {
			String[] titleAndLink = title.split("\\|");
			if (titleAndLink.length == 1)
				continue;

			cTitle = titleAndLink[0];
			cUrl = titleAndLink[1];

			profile = new ProfilesIni();
			testProfile = profile.getProfile("newuser");
			testProfile.setPreference("dom.webnotifications.enabled", false);
			testProfile.setPreference("dom.push.enabled", false);
			dc = DesiredCapabilities.firefox();
			dc.setCapability(FirefoxDriver.PROFILE, testProfile);
			opt = new FirefoxOptions();
			opt.merge(dc);
			driver = new FirefoxDriver(opt);

			driver.get(cUrl);
			TimeUnit.SECONDS.sleep(3);

			List<WebElement> elements = new ArrayList<WebElement>();
			if (cTitle.equals("Asterix")) {
				elements = driver.findElements(By.xpath("//div[@class='post-body entry-content']/a"));
			} else if (cTitle.equals("LuckyLuke")) {
				elements = driver.findElements(By.xpath("//div[@class='post-body entry-content']/div/a"));
			}

			System.out.println(elements.size() + " chapters.");
			
			for (WebElement ele : elements) {
				System.out.println(ele.getText());
				System.out.println(ele.getAttribute("href"));
				
				listOfCompleted = readFromFiles("./listOfCompleted.txt");

				
				if (listOfCompleted.contains(ele.getText())) {
					continue;
				} else {
					appendOneLine(ele.getText(), "./listOfCompleted.txt");
					WebDriver subDriver = new FirefoxDriver(opt);
					subDriver.get(ele.getAttribute("href"));

					List<WebElement> linkToEachFrame = new ArrayList<WebElement>();
					linkToEachFrame = subDriver.findElements(By.xpath("//div[@class='read']/div/a"));
					
					
					if (linkToEachFrame.isEmpty()) {
						System.out.println(ele.getText() + " is Error!");
					}
					
					int page = 1;
					for(WebElement pics : linkToEachFrame) {
						saveImageTo(pics.getAttribute("href"), cTitle, ele.getText(), String.valueOf(page));
						page++;
					}
					
					subDriver.close();
				}

			}

		}
	}

	private static List<String> readFromFiles(String url) {
		try {
			List<String> allLines = Files.readAllLines(Paths.get(url));
			return allLines;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void appendOneLine(String _text, String url) {
		try {
			FileWriter fw = new FileWriter(url, true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(_text);
			bw.newLine();
			bw.close();
		} catch (Exception e) {
		}
	}
	
	private static void saveImageTo(String src, String title, String chapter, String page) {
		
		try {
			BufferedImage bufferedImage = ImageIO.read(new URL(src));
			File outputfile = new File(System.getProperty("user.dir") + "/content/" + title + "/" + chapter + "/" + chapter + "-" + page +  ".png");
			File folder  = outputfile.getParentFile();
			if (!folder.exists())
				folder.mkdirs();
			ImageIO.write(bufferedImage, "png", outputfile);
		}catch(Exception e) {
			
		}
	}

}
