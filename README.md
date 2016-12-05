## Cronjob

    
一个强大的linux定时任务调度系统.一个正在可用的linux定时任务调度定系统,多种场景下各种复杂的定时任务调度,同时集成了linux实时监控,以图形化的方式展示出来,同时集成了webssh.

你是否有定时执行任务计划的需求,需要在linux的crontab里一一定义任务?如何有你一定有以下难题:
 -  需要在每台linux服务器的crontab里一一定义任务
 -  任务的执行监控太不方便了
 -  得登录到每台机器查看定时任务的运行结果,机器一多简直是一种灾难
 -  对于多台机器协同处理一个任务很麻烦,如何保证多台机器上的任务按顺序依次执行?
 -  当任务运行失败,要重新执行,还得重新定义下执行时间,让其重跑,重跑完成了还得改回正常时间
 -  正在运行的任务要kill掉很麻烦,查看进程然后才能kill
......

Cronjob的出现将彻底的解决上面所有问题,是一个功能完备的真正通用的调度系统.功能如下:
 -  自动化管理任务,提供可操作的web图形化管理
 -  要当场执行只需点击执行即可,非常方便
 -  时间规则支持quartz和crontab,时间规则更强大更灵活
 -  非常方便的修改任务的执行时间
 -  任务的运行状态实时查看
 -  支持一键kill掉当前运行的任务(已经当前任务调起的其他子任务,彻底kill)
 -  支持重新执行正在运行的任务
 -  出错后实时通知给任务人(超过重跑次数自动发送邮件,短信)
 -  支持任务超时设置,一旦超过预定运行时长自动kill,任务结束,防止僵尸任务
 -  支持流程任务(多台机器上的任务协同完成一个大的任务,按任务定义的顺序依次执行)
 -  记录任务的运行日志,非常方便查看
 -  多用户多角色
 -  现场执行(选择N台机器同时执行一个命令或任务)
 -  webssh,执行在浏览器一键登录到linux服务器
 -  提供服务器的性能实时监控
......
    
   

## 运行环境


    Java JDK 1.7 or greater
    http://www.oracle.com/technetwork/java/javase/overview/index.html

    Tomcat server 7.0 or greater
    https://tomcat.apache.org

    Nginx 1.3 or greater
    http://nginx.org
    
    Browser 
    IE10+
   
   
## 项目截图   

![Main](https://github.com/wolfboys/cronjob/blob/master/img/main.png)

![Agent](https://github.com/wolfboys/cronjob/blob/master/img/agent.png)

![Job](https://github.com/wolfboys/cronjob/blob/master/img/job.png)

![Record](https://github.com/wolfboys/cronjob/blob/master/img/record.png)

![Terminal](https://github.com/wolfboys/cronjob/blob/master/img/terminal.png)


  
## 安装步骤


    cronjob分为两个cronjob-server端和cronjob-agent端，cronjob-server端即为一个web可视化的中央管理调度平台,cronjob-agent为要管理的任务的机器,
    每个要纳入中央统一管理的机器都必须安装cronjob-agent, cronjob-agent在要管理的服务器中安装执行完后，可以直接在cronjob-server添加当前的机器.


## Cronjob-agent 安装步骤:


    1)下载编译源码，在cronjob-agent的target下会生成一个在cronjob-agent-${version}.tar.gz的包
    
    2)部署agent，将cronjob-agent-${version}.tar.gz包拷贝到要管理任务的目标服务器,解包,会看到以下目录
     ---bin/
         |  startup.sh         #agent的启动脚本,调用的是cronjob.sh来完成
         |  shutdown.sh        #agent停止脚本，调用的是cronjob.sh来完成
         |  cronjob.sh         #agent控制启动|停止的脚本
         |  monitor.sh         #实时监控获取数据需要的脚本,由系统调度
         |  kill.sh            #kill任务时需要的脚本,由系统调度
     ---conf/
         | log4j.properties    #log4j配置文件
     ---lib/
         | *.jar               #agent运行需要的jar文件
     ---temp/
         | *.sh                #用于存放项目生成的零时文件的目录
     ---logs
         | cronjob.out         #项目启动会产生的Log文件
     
     > tar -xzvf cronjob-agent-${version}.tar.gz
    3)启动cronjob-agent 进入cronjob-agent/bin
     > cd cronjob-agent/bin
     > sh startup.sh
     这里可以接受两个参数，分别是服务启动的端口和密码，默认端口是:1577,默认密码:cronjob
     如要指定参数启动命令如下:
     > sh startup.sh -P10001 -p123456
     参数说明:
      -P (大写的p)为agent启动的端口，选填，如果不输入默认启动端口是1577
      -p (小写的p)为当前agent的连接密码,选填，如果不输入默认连接该机器的密码是cronjob
    启动完后会看到有一个cronjob.pid，这个文件记录了当前agent进程的pid.更多详细的启动，运行日志请查看logs/cronjob.out
       
    4)停止cronjob-agent 进入cronjob-agent/bin 执行：
      > cd cronjob-agent/bin
      > sh shutdown.sh
     
     
## Cronjob-server 部署步骤:


     1):导入初始化sql,在mysql里导入setup.sql文件
     >mysql -uroot -proot   --进入mysql
     >source /usr/local/setup.sql;
     没啥意外就导入成功了,导入完毕会看到多了一个cronjob的库
     2):更改项目的mysql连接信息和memcached连接，配置文件有两套，一个是pord一个是test,
     默认读取的是test下的配置文件,prod下的资源文件maven编译是以"-Ponline"激活，
     mysql和memcached配置在config.properties里，更改即可:
     如:
     --mysql
     jdbc.driver=com.mysql.jdbc.Driver
     jdbc.url=jdbc:mysql://${you_mysql_host}:3306/cronjob?useUnicode=true&characterEncoding=UTF-8&useCursorFetch=true&autoReconnect=true&failOverReadOnly=false
     jdbc.username=${user}
     jdbc.password=${password}
    
     --memcached
      memcached.server=${memcached_host}:${memcached_port}
     3)maven编译完项目在cronjob-server模块下有个cronjob-server-${version}.war
     将这个war包发布到tomcat或者其他的web服务器启动即可.
     
     默认初始用户名cronjob,密码cronjob
     进入到cronjob的管理端第一件要做的事情就是添加要管理的执行器.
     在菜单的第二栏点击"执行器管理"->添加执行器
     执行器ip，就是上面你部署的cronjob-agent的机器ip，端口号是要连接的cronjob-agent的启动端口，密码也是cronjob-agent端的连接密码
     输入ip,端口和密码后点击"检查通信",如果成功则server和agnet端已经成功通信，server可以管理agent了,添加保持即可.如果连接失败，
     可能有一下即可原因:
     1):agent端启动失败,检查logs,查看详情
     2):检查agent端口是否开放(如很多云服务器得开放端口才能访问)
  
## 注意:
      cronjob-agent端编译后如果bin/下的脚本不能成功执行。如 startup.sh脚本不能执行,则:
      >vi startup.sh
      然后查看当前脚本的字符集
      :set ff 回车 （命令模式下,会显示:fileformat=unix或者其他,出问题都是非unix的字符集，然后将字符集改成unix即可)
      :set ff=unix 回车 (保存退出即可)
    
      以上是简单的安装部署,更多任务的管理使用后续会出详细的文档.
      
      cronjob交流群156429713,欢迎大家加入
        
    
    
    
