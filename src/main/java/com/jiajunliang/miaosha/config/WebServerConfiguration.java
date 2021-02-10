package com.jiajunliang.miaosha.config;

import com.alibaba.druid.support.http.WebStatFilter;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * @project: MiaoshaProject
 * @program: WebServerConfiguration
 * @description:
 * @author: JIAJUN LIANG
 * @create: 2021-02-09 21:49
 **/
//当Spring容器内没有TomcatEmbeddedServletContainerFactory这个bean时，把此bean加载进Spring容器
@Component
public class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        //使用工厂类factory提供的接口定制化tomcat connector
        ((TomcatServletWebServerFactory)factory).addConnectorCustomizers(new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {
                Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
                //定制化keepAliveTimeOut,30秒内没有请求则服务端自动断开keepAlive连接
                protocol.setKeepAliveTimeout(30000);
                //定制化maxKeepAliveRequests,当客户端发送超过10000个请求时自动断开keepAlive连接
                protocol.setMaxKeepAliveRequests(10000);
            }
        });
    }
}
