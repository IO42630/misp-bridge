package core;

import exchange.ExchangeMock;

import javax.servlet.ServletException;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Wraps a ClientServlet so it can be debugged easily, i.e. without running Tomcat.
 */
public class ClientMock extends ClientServlet {

    private MockSet mockSet;

    public ClientMock(MockSet mockSet){
        super();
        mockSet.clientMock = this;
        this.mockSet = mockSet;
    }

    @Override
    void sendPostRide(Ride ride) throws IOException, ServletException, InterruptedException {

        rideMap.put(ride.getID(), ride.setState(State.AVAILABLE));


        // Mock Exchange
        ExchangeMock exchange = new ExchangeMock();

        exchange.request.setMethod("POST");
        exchange.request.setContentType("application/json");
        exchange.request.setContent(ride.json().getBytes());

        synchronized (exchange){
            // Mock POST (Ride)
            exchange.notify();
            mockSet.bridgeMock.doPost(exchange.request,exchange.response);
            exchange.wait();

            // handle OK (Ride)(Request)
            Ride parsedRide = new Ride(exchange.response.getContentAsString());
            ride.setRequest(parsedRide.getRequest());
            ride.setState(State.BOOKED);
            sendGetRequest(ride);
        }



    }


    /**
     * # send GET (Request) to App
     */
    @Override
    void sendGetRequest(Ride ride) throws IOException {

        HttpURLConnection connection = ConnectionHelper.make("GET", ClientServlet.APP_URL);

        // send GET (Request)
        connection.setDoOutput(true);
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(ride.getRequest());
        outputStream.flush();
        outputStream.close();

        // handle OK (Data)
        // remove Ride from BookedRides
        // add Ride to LoadedRides
        // send GET (Ride)(Data)
        if (connection.getResponseCode() == 200) {
            String parsedData = ConnectionHelper.parseString(connection);
            ride.setData(parsedData);
            ride.setState(State.LOADED);
        }

        sendGetRideRequestData(ride);




    }


    /**
     * # send GET (Ride)(Request)(Data)
     */
    @Override
    void sendGetRideRequestData(Ride ride) throws IOException {

        HttpURLConnection connection = ConnectionHelper.make("GET", ClientServlet.MISP_BRIDGE_URL);

        // send GET (Ride)(Request)(Data)
        connection.setDoOutput(true);
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(ride.json());
        outputStream.flush();
        outputStream.close();

        // handle OK (Ride)
        // remove Ride from LoadedRides
        if (connection.getResponseCode() == 200) {
            Ride shellIdRide = ConnectionHelper.parseRide(connection);
            if (shellIdRide.getID() != null) {
                rideMap.remove(ride.getID());
            }
        }
    }

}