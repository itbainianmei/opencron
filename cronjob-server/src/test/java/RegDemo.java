import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.jcronjob.base.job.CronJob;
import org.jcronjob.base.job.Monitor;
import org.jcronjob.base.utils.CommandUtils;
import org.jcronjob.base.utils.ReflectUitls;
import org.jcronjob.domain.Term;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by benjobs on 15/12/5.
 */
public class RegDemo {

    public static void main(String[] args) throws Exception {
        Term console = new Term();
        console.setHost("120.132.92.16");
        console.setPassword("blueapi123!@#");
        console.setUser("root");
        console.setPort(22);

        JSch jsch = new JSch();
        Session jschSession = jsch.getSession(console.getUser(), console.getHost(), console.getPort());
        jschSession.setPassword(console.getPassword());
        java.util.Properties config = new java.util.Properties();
        //不记录本次登录的信息到$HOME/.ssh/known_hosts
        config.put("StrictHostKeyChecking", "no");
        jschSession.setConfig(config);
        jschSession.connect();

        Channel channel = jschSession.openChannel("exec");

        ((ChannelExec) channel).setCommand("cd ~/");
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(System.err);

        channel.connect();
        InputStream in = channel.getInputStream();


        BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
        String buf = null;
        while ((buf = reader.readLine()) != null) {
            System.out.println(buf);
        }
        reader.close();
        channel.disconnect();
        jschSession.disconnect();
    }
}
