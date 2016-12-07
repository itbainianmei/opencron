/**
 * Copyright 2016 benjobs
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.jcronjob.server.controller;

import com.alibaba.fastjson.JSON;
import com.jcronjob.common.utils.*;
import com.jcronjob.server.domain.Job;
import com.jcronjob.server.domain.User;
import com.jcronjob.server.job.Globals;
import com.jcronjob.common.job.Cronjob;
import com.jcronjob.common.job.Response;
import com.jcronjob.server.domain.Agent;
import com.jcronjob.server.tag.Page;
import com.jcronjob.server.vo.ChartVo;
import static  com.jcronjob.server.service.TerminalService.*;
import com.jcronjob.server.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.jcronjob.common.utils.CommonUtils.isEmpty;
import static com.jcronjob.common.utils.CommonUtils.notEmpty;

/**
 * Created by ChenHui on 2016/2/17.
 */
@Controller
public class HomeController {

    @Autowired
    private HomeService homeService;

    @Autowired
    private RecordService recordService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private ExecuteService executeService;

    @Autowired
    private JobService jobService;

    @Autowired
    private UserService userService;


    private Logger logger = LoggerFactory.getLogger(getClass());

    @RequestMapping("/home")
    public String index(HttpSession session, Model model) {

        /**
         * agent...
         */
        List<Agent> success = agentService.getAgentByStatus(1,session);
        List<Agent> failed = agentService.getAgentByStatus(0,session);
        model.addAttribute("success",success.size());
        model.addAttribute("failed",failed.size());

        success.addAll(failed);
        model.addAttribute("agents",success);

        /**
         * job
         */
        List<Job> singleton = jobService.getJobsByJobType(Cronjob.JobType.SINGLETON, session);
        List<Job> flow = jobService.getJobsByJobType(Cronjob.JobType.FLOW, session);

        model.addAttribute("singleton",singleton.size());
        model.addAttribute("flow",flow.size());
        model.addAttribute("job",singleton.size()+flow.size());

        /**
         * 成功作业,自动执行
         */
        Long successAutoRecord = recordService.getRecords(1, Cronjob.ExecType.AUTO, session);
        Long successOperRecord = recordService.getRecords(1, Cronjob.ExecType.OPERATOR, session);

        model.addAttribute("successAutoRecord",successAutoRecord);
        model.addAttribute("successOperRecord",successOperRecord);
        model.addAttribute("successRecord",successAutoRecord+successOperRecord);

        /**
         * 失败作业
         */
        Long failedAutoRecord = recordService.getRecords(0, Cronjob.ExecType.AUTO, session);
        Long failedOperRecord = recordService.getRecords(0, Cronjob.ExecType.OPERATOR, session);
        model.addAttribute("failedAutoRecord",failedAutoRecord);
        model.addAttribute("failedOperRecord",failedOperRecord);
        model.addAttribute("failedRecord",failedAutoRecord+failedOperRecord);


        model.addAttribute("startTime", DateUtils.getCurrDayPrevDay(7));
        model.addAttribute("endTime", DateUtils.formatSimpleDate(new Date()));

        return "/home/index";
    }

    @RequestMapping("/record")
    public void record(HttpServletResponse response,HttpSession session, String startTime, String endTime) {
        if (isEmpty(startTime)) {
            startTime = DateUtils.getCurrDayPrevDay(7);
        }
        if (isEmpty(endTime)) {
            endTime = DateUtils.formatSimpleDate(new Date());
        }
        //成功失败折线图数据
        List<ChartVo> voList = recordService.getRecord(startTime, endTime, session);
        if (isEmpty(voList)) {
            WebUtils.writeJson(response, "null");
        }else {
            WebUtils.writeJson(response, JSON.toJSONString(voList));
        }
    }

    @RequestMapping("/monitor")
    public void port(HttpServletResponse response, Long agentId) throws Exception {
        Agent agent = agentService.getAgent(agentId);
        Response req = executeService.monitor(agent);
        /**
         * 直联
         */

        String format = "%d_%s";

        if (agent.getProxy().equals(Cronjob.ConnType.CONN.getType())) {
            String port = req.getResult().get("port");
            String url = String.format("http://%s:%s", agent.getIp(), port);
            WebUtils.writeHtml(response, String.format(format,agent.getProxy(), url));
        } else {//代理
            WebUtils.writeHtml(response, String.format(format,agent.getProxy(),JSON.toJSONString(req.getResult())) );
        }
    }

    @RequestMapping("/login")
    public void login(HttpServletRequest request,HttpServletResponse response, HttpSession httpSession, @RequestParam String username, @RequestParam String password) throws Exception {
        //用户信息验证
        int status = homeService.checkLogin(httpSession, username, password);

        if (status == 500) {
            WebUtils.writeJson(response, "{\"msg\":\"用户名密码错误\"}");
            return;
        }
        if (status == 200) {
            User user = (User) httpSession.getAttribute(Globals.LOGIN_USER);

            //提示用户更改默认密码
            byte[] salt = Encodes.decodeHex(user.getSalt());
            byte[] hashPassword = Digests.sha1(DigestUtils.md5Hex("cronjob").toUpperCase().getBytes(), salt, 1024);
            String hashPass = Encodes.encodeHex(hashPassword);

            String format = "{\"status\":\"%s\",\"%s\":\"%s\"}";

            if (user.getUserName().equals("cronjob") && user.getPassword().equals(hashPass)) {
                WebUtils.writeJson(response,String.format(format,"edit","userId",user.getUserId()) );
                return;
            }

            if (user.getHeaderpic()!=null) {
                String name = user.getUserId() + "_140"+user.getPicExtName();
                String path = httpSession.getServletContext().getRealPath(File.separator) + "upload" + File.separator + name;
                IOUtils.writeFile(new File(path), user.getHeaderpic().getBinaryStream());
                user.setHreaderPath(WebUtils.getWebUrlPath(request)+"/upload/"+name);
            }
            WebUtils.writeJson(response,   String.format(format,"success","url","/home"));
            return;
        }
    }


    @RequestMapping("/logout")
    public String logout(HttpSession httpSession) throws IOException {
        User user = (User) httpSession.getAttribute(Globals.LOGIN_USER);

        //用户退出后当前用户的所有终端全部退出.
        TerminalSession.exit(user);

        httpSession.removeAttribute(Globals.LOGIN_USER);
        return "redirect:/";
    }

    @RequestMapping("/headpic/upload")
    public void upload(@RequestParam(value = "file", required = false) MultipartFile file,@RequestParam Long userId,HttpServletRequest request, HttpSession httpSession, HttpServletResponse response) throws Exception {
        String extensionName = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);

        String successFormat = "{\"result\":\"%s\",\"state\":200}";
        String errorFormat = "{\"message\":\"%s\",\"state\":500}";

        //检查后缀
        if(!"BMP,JPG,JPEG,PNG,GIF".contains(extensionName.toUpperCase())) {
            WebUtils.writeJson( response, String.format(errorFormat,"格式错误,请上传(bmp,jpg,jpeg,png,gif)格式的图片"));
            return;
        }

        User user = userService.getUserById(userId);

        if (user==null) {
            WebUtils.writeJson( response, String.format(errorFormat,"用户信息获取失败"));
            return;
        }

        String path = httpSession.getServletContext().getRealPath("/")+"upload"+File.separator;

        String fileName = user.getUserId()+"_pic." + extensionName.toLowerCase();
        File targetFile = new File(path, fileName);
        if (!targetFile.exists()) {
            targetFile.mkdirs();
        }

        try {
            file.transferTo(targetFile);
            //检查文件是不是图片
            Image image= ImageIO.read(targetFile);
            if (image == null) {
                WebUtils.writeJson( response, String.format(errorFormat,"格式错误,正确的图片"));
                targetFile.delete();
                return;
            }

            //检查文件大小
            if (targetFile.length()/1024/1024 > 5) {
                WebUtils.writeJson( response, String.format(errorFormat,"文件错误,上传图片大小不能超过5M"));
                targetFile.delete();
                return;
            }

            //保存入库.....
            userService.uploadimg(targetFile,userId);

            String contextPath = WebUtils.getWebUrlPath(request);
            String imgPath = contextPath+"/upload/"+fileName+"?"+System.currentTimeMillis();
            user.setHreaderPath(imgPath);
            user.setHeaderpic(null);
            httpSession.setAttribute(Globals.LOGIN_USER,user);

            WebUtils.writeJson( response, String.format(successFormat,imgPath));
            logger.info(" upload file successful @ "+fileName);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("upload exception:"+e.getMessage());
        }
    }

    @RequestMapping("/notice/view")
    public String log(HttpSession session, Model model, Page page, Long agentId, String sendTime) {
        model.addAttribute("agents", agentService.getAgentsBySession(session));
        if (notEmpty(agentId)) {
            model.addAttribute("agentId", agentId);
        }
        if (notEmpty(sendTime)) {
            model.addAttribute("sendTime", sendTime);
        }
        homeService.getLog(session,page, agentId, sendTime);
        return "notice/view";
    }


    @RequestMapping("/notice/uncount")
    public void uncount(HttpSession session, HttpServletResponse response) {
         Long count = homeService.getUnReadCount(session);
         WebUtils.writeHtml(response,count.toString());
    }

    /**
     * 未读取的站类信
     * @param session
     * @param model
     * @return
     */
    @RequestMapping("/notice/unread")
    public String nuread(HttpSession session, Model model) {
        model.addAttribute("message",homeService.getUnReadMessage(session));
        return "notice/info";
    }

    @RequestMapping("/notice/detail")
    public String detail(Model model, Long logId) {
        model.addAttribute("sender", configService.getSysConfig().getSenderEmail());
        model.addAttribute("log", homeService.getLogDetail(logId));
        homeService.updateAfterRead(logId);
        return "notice/detail";
    }
}

