package cn.inxiny.live.gain;

import cn.inxiny.live.Util.DBUtil;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class GainController {

    @GetMapping(value = "/hello")
    public Integer say ( ){
        return 1;
    }

    // jsoup爬取虎牙
    @GetMapping(value = "/huyaJsoup")
    public void gainHuYaJsoup ( ) {
        String url="https://www.huya.com/g/wzry";//分类的首页
        Connection conn = Jsoup.connect(url);
        try {
            Document doc= conn.userAgent("Mozilla").timeout(3000).get();
            //Elements links = doc.getElementsByClass("avatar fl");//主播昵称
            //Elements links = doc.getElementsByClass("txt");//可以遍历到人数和主播昵称
            //Elements links = doc.getElementsByClass("title new-clickstat");//链接和标题

            Elements links = doc.getElementsByClass("game-live-item");//可以遍历到人数标题 是最大的类

            for(Element link: links)//遍历链接
            {
                /*
                 * 链接在这个html文档中的子类title new-clickstat中在分析这个元素
                 */
                Document doe=Jsoup.parse(link.html());
                Elements e2 =doe.getElementsByClass("title new-clickstat");
                System.out.print(e2.attr("href"));

                String a=link.attr("href");//href链接
                String b=link.text();	//内容文字
                int c=Integer.parseInt(link.attr("gid"));
                System.out.print(b + " : \t");
                System.out.println(c);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // htmlunit爬取虎牙
    @GetMapping(value = "/huyaHtmlunit")
    public void gainHuYaHtmlunit ( ) {

        final WebClient webClient = new WebClient(BrowserVersion.CHROME);

        webClient.getOptions().setTimeout(2000);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setActiveXNative(false);
        webClient.getOptions().setCssEnabled(false);

        webClient.waitForBackgroundJavaScript(600*1000);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());

        try {
            HtmlPage page=webClient.getPage("https://www.huya.com/g/wzry");
            List<HtmlAnchor> anchors = page.getAnchors();
            List<HtmlAnchor> jsanchors = new ArrayList();

            for(HtmlAnchor anchor:anchors){
                String href=anchor.getAttribute("href");
                if(href.startsWith("javascript:"))
                {
                    jsanchors.add(anchor);
                }
                else if("#".equals(href))
                {
                    if(anchor.hasAttribute("onclick"))
                    {
                        jsanchors.add(anchor);
                    }
                }

            }
            HtmlAnchor t1 = jsanchors.get(0);
            HtmlPage newpage = t1.click();

            Thread.sleep(1000);

            System.out.println(newpage.asText());


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    // Json爬取虎牙
    @GetMapping(value = "/huyaJson")
    public String gainHuYaJson ( ) {
        String url="https://www.huya.com/";
        Connection connect = Jsoup.connect(url).timeout(20000);
        Map header = new HashMap();
        header.put("m", "LiveList");
        header.put("do", "getLiveListByPage");
        header.put("tagAll", "0");
        header.put("page", "1");
        int page = 0;//页数
        connect.data(header);
        Document doc= null;
        try {
            //获取页面数操作 首先要用jsoup解析当前页面，看看有多少页，
            Document exam = Jsoup.connect("https://www.huya.com/l").get();//请求链接所有直播
            Elements links = exam.getElementsByClass("list-page");//可以遍历到人数和主播昵称
            page = Integer.parseInt(links.attr("data-pages"));
            System.out.println(page);

            doc = connect.get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String text = doc.text();

        return text;
    }

    // Jsoup爬取斗鱼
    @GetMapping(value = "/douyuJsoup")
    public String gainDouyuJsoup ( ) {
        try {
            // 获取列表数
            /*final WebClient webClient = new WebClient(BrowserVersion.CHROME);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setCssEnabled(false);
            HtmlPage page = webClient.getPage("https://www.douyu.com/directory/all");
            webClient.waitForBackgroundJavaScript(5000);*/

            //  转换成xml，使用Jsoup解析
            /*String pageXml = page.asXml();
            Document document = Jsoup.parse(pageXml);
            List<Element> infoListEle = document.getElementById("feedCardContent").getElementsByAttributeValue("class", "feed-card-item");*/


            // 获取列表信息
            String url = "https://www.douyu.com/gapi/rkc/directory/0_0/1";
            Connection connect = Jsoup.connect(url).ignoreContentType(true).timeout(20000);

            Document doc = connect.get();
            return doc.text();
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }


    }

}
