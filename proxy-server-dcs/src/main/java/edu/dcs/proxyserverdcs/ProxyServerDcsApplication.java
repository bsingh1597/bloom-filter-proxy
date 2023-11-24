package edu.dcs.proxyserverdcs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import edu.dcs.proxyserverdcs.server.ProxyServer;

@SpringBootApplication
public class ProxyServerDcsApplication {

	public static void main(String[] args) {
		
        ApplicationContext context = SpringApplication.run(ProxyServerDcsApplication.class, args);

        ProxyServer proxy = context.getBean(ProxyServer.class);

        Thread publisherThread = new Thread(() -> proxy.multicastPublisher());
        publisherThread.start();
        Thread receiverThread = new Thread(() -> proxy.multicastReciever());
        receiverThread.start();
	}

}
