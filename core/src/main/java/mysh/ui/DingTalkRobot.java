package mysh.ui;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.api.client.json.Json;
import com.google.common.base.Charsets;
import mysh.collect.Colls;
import mysh.net.httpclient.HttpClientAssist;

import java.io.IOException;

/**
 * ding talk robot api
 * <p>
 * https://open-doc.dingtalk.com/docs/doc.htm?source=search&treeId=257&articleId=105735&docType=1
 *
 * @author <pre>凯泓(zhixian.zzx@alibaba-inc.com)</pre>
 * @since 2018/08/28
 */
public class DingTalkRobot {
    private static final HttpClientAssist hca = new HttpClientAssist();

    private final String webhook;

    public DingTalkRobot(String webhook) {
        this.webhook = webhook;
    }

    /**
     */
    public void sendTextMsg(String content, boolean atAll, String... atMobiles) {
        String json = JSON.toJSONString(Colls.ofHashMap(
                "msgtype", "text",
                "text", Colls.ofHashMap("content", content),
                "at", Colls.ofHashMap("atMobiles", atMobiles, "isAtAll", atAll)
        ));

        sendMsg(json);
    }

    /**
     * @param title    show only in mobile system notify
     * @param markdown markdown content. support a subset markdown grammar.
     */
    public void sendMarkdownMsg(String title, String markdown, boolean atAll, String... atMobiles) {
        String json = JSON.toJSONString(Colls.ofHashMap(
                "msgtype", "markdown",
                "at", Colls.ofHashMap("atMobiles", atMobiles, "isAtAll", atAll),
                "markdown", Colls.ofHashMap("title", title, "text", markdown)
        ));

        sendMsg(json);
    }

    private void sendMsg(String json) {
        try (HttpClientAssist.UrlEntity ue = hca.accessPostBytes(webhook, null, Json.MEDIA_TYPE, json.getBytes(Charsets.UTF_8))) {
            String rj = ue.getEntityStr();
            if (ue.getStatusCode() != 200) {
                throw new RuntimeException("sendMsg-httperr-:" + ue.getStatusCode() + ":" + rj);
            }
            JSONObject rjson = JSON.parseObject(rj);
            int errcode = rjson.getIntValue("errcode");
            if (errcode != 0) {
                throw new RuntimeException("sendMsg-err-" + errcode + ":" + rjson.getString("errmsg"));
            }
        } catch (IOException e) {
            throw new RuntimeException("sendMsg-ioexp-" + e.getMessage(), e);
        }
    }
}
