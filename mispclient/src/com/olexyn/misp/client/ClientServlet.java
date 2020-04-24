package com.olexyn.misp.client;

import com.olexyn.misp.helper.Ride;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;


public class ClientServlet extends HttpServlet {

    protected static final String MISP_BRIDGE_URL = "http://localhost:9090/mispbridge/core";
    protected static final String APP_URL = "http://localhost:9090/mispclient";

    public static final int AVAILABLE_RIDES_OVERHEAD_TRIGGER = 2;
    public static final int AVAILABLE_RIDES_OVERHEAD = 4;


    public final Map<Long, Ride> available = new HashMap<>();
    public final Map<Long, Ride> booked = new HashMap<>();
    public final Map<Long, Ride> loaded = new HashMap<>();


    public ClientServlet() {

        // Thread : while AvailableRides < 256 , add Ride to AvailableRides , send POST (Ride) [DONE]
        Thread postRideThread = new Thread(new PostRideRunnable(this));
        postRideThread.setName("postRideThread");
        postRideThread.start();
    }

    /**
     * Debugging
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter print = response.getWriter();

        print.println("<!DOCTYPE html>");

        print.println("<html lang=\"en\">");
        print.println("<head>");
        print.println("<meta charset=\"utf-8\">");
        print.println("<title>title</title>");
        print.println("<link rel=\"stylesheet\" href=\"style.css\">");
        print.println("<script src=\"script.js\"></script>");
        print.println("</head>");
        print.println("<body>");
        print.println(Debug.mapTables(this));

        print.println(" </body></html>");


    }

    /**
     * Generated by Loop.
     * Prepare payload for the request.
     * Process the parsed response.
     */
    final void sendPostRide() throws IOException, ServletException, InterruptedException {

        final Ride ride = new Ride();

        synchronized (available) {
            available.put(ride.getID(), ride);
        }

        final Ride parsedRide = doSendPostRide(ride);

        synchronized (available) {
            available.remove(ride.getID());
            ride.setRequest(parsedRide.getRequest());
        }

        synchronized (booked) {
            booked.put(ride.getID(), ride);
        }
        sendGetRequest(ride);
    }


    /**
     * Send POST (Ride).
     * Parse response.
     */
    protected Ride doSendPostRide(Ride ride) throws IOException, ServletException, InterruptedException {
        // send POST (Ride)
        final HttpURLConnection connection = ConnectionHelper.make("POST", MISP_BRIDGE_URL);

        connection.setDoOutput(true);
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(ride.json());
        outputStream.flush();
        outputStream.close();

        Ride rideXa= ConnectionHelper.parseRide(connection);
        rideXa.setRequest("ff");
        return rideXa;
    }


    /**
     * Prepare payload for the request.
     * Process the parsed response.
     */
    final void sendGetRequest(Ride ride) throws IOException, ServletException, InterruptedException {


        ride.setData(doSendGetRequest(ride.getRequest()));

        synchronized (booked) {
            booked.remove(ride.getID());
        }
        synchronized (loaded) {
            loaded.put(ride.getID(), ride);
        }

        sendGetRideRequestData(ride);
    }


    /**
     * Send GET (Request) to App.
     * Parse response.
     */
    protected String doSendGetRequest(String request) throws IOException, InterruptedException {

        // send GET (Ride)
        final HttpURLConnection connection = ConnectionHelper.make("GET", APP_URL);

        connection.setDoOutput(true);
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(request);
        outputStream.flush();
        outputStream.close();

        return ConnectionHelper.parseString(connection);
    }


    /**
     * Prepare payload for the request.
     * Process the parsed response.
     */
    final protected void sendGetRideRequestData(Ride ride) throws IOException, InterruptedException {
        doSendGetRideRequest(ride);
        synchronized (loaded) {
            loaded.remove(ride.getID());
        }
    }


    /**
     * Send GET (Ride)(Request)(Data).
     * Parse response.
     */
    protected void doSendGetRideRequest(Ride ride) throws IOException, InterruptedException {

        HttpURLConnection connection = ConnectionHelper.make("GET", MISP_BRIDGE_URL);

        connection.setDoOutput(true);
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(ride.json());
        outputStream.flush();
        outputStream.close();
    }
}


/**
 * While AvailableRides < 256 ,
 * add Ride to AvailableRides ,
 * send POST (Ride).
 */
class PostRideRunnable implements Runnable {

    ClientServlet clientServlet;

    public PostRideRunnable(ClientServlet clientServlet) {
        this.clientServlet = clientServlet;
    }

    @Override
    public void run() {
        while (true) {
            if (clientServlet.available.size() < ClientServlet.AVAILABLE_RIDES_OVERHEAD_TRIGGER) {
                for (int i = 0; i < ClientServlet.AVAILABLE_RIDES_OVERHEAD; i++) {
                    try {clientServlet.sendPostRide();} catch (IOException | ServletException | InterruptedException e) { e.printStackTrace(); }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

