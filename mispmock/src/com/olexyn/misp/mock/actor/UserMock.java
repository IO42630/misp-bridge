package com.olexyn.misp.mock.actor;

import com.olexyn.misp.mock.MockSet;
import com.olexyn.misp.mock.exchange.ExchangeMock;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;




public class UserMock extends ActorRunnable {



    final String longRequest;

    int requestCount = 0;

    public UserMock(MockSet mockSet){
        super(mockSet);
        mockSet.userMock = this;


        StringBuilder sb = new StringBuilder();
        for (int i=0;i<100;i++){
            sb.append("foo");
        }
        longRequest = sb.toString();


    }

    @Override
    public void run() {
        while (true){
            try {
                sendGetRequest();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) {

    }



    /**
     * # send GET (Request)
     * Generated by Loop
     */
    void sendGetRequest() throws IOException, InterruptedException {

        // Mock Exchange
        ExchangeMock exchange = new ExchangeMock();

        exchange.request.setMethod("GET");
        //exchange.request.setContentType("application/json");

        //String requestBody = longRequest+"-"+(++requestCount);
        String requestBody = "REQUEST-"+(++requestCount);
        String jsonString = "{\"request\":\""+requestBody+ "\"}";
        jsonString = "asdfasdfa";
        exchange.request.setContent(jsonString.getBytes());

        synchronized (exchange){
            // Mock GET (Request)
            exchange.notify();
            mockSet.bridgeMock.doGet(exchange.request,exchange.response);
            exchange.wait();

            // handle OK (Data)
            String data = exchange.response.getContentAsString();
            System.out.println(data + " of "+requestBody);
            exchange.notify();
        }
    }

}
