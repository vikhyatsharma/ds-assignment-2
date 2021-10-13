import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.ArrayList;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;

// main class GETClient
public class GETClient {

    private int count = 0; // This variable represent how many times client has tried
    private LamportClock clock = new LamportClock(0); // Lamport clock for this class

    // This function gets the data from the server and converts it into an array of string and returns it back to the main function

    public String[] create_object(String servername, String portnumber) {

        String Request_response = new String();
        String Request_response_and_lamport = new String();
        this.count += 1;    // Update count everytime new connection is requested
        
        try{

            Socket ss = new Socket(servername, Integer.parseInt(portnumber)); // Creating connection with the Server

            DataInputStream input_from_server = new DataInputStream(ss.getInputStream());
            DataOutputStream output_to_server = new DataOutputStream(ss.getOutputStream());

            this.clock.increment_clock();
            String send = "GET"+"LamportClock:"+ this.clock.get_LamportClock();
            output_to_server.writeUTF(send); //Sending the get Request with lamport clock
            

            Request_response_and_lamport = input_from_server.readUTF();
            Request_response = Request_response_and_lamport.split("LamportClock:")[0];
            this.clock.tick(Integer.parseInt(Request_response_and_lamport.split("LamportClock:")[1]));// Updating the lamport clock and recieving the response

            // Closing the connection
            input_from_server.close();
            output_to_server.close();
            ss.close();
            

        } catch (IOException e) { // IF an error occured we are going to try three times before giving up
            System.out.println("An error occurred.");
            e.printStackTrace();

            if(this.count <= 3) { // If count is less than or equal to 3 we will call this function recursively incrementing the count every time
                System.out.println("Retrying to connect with the Server\n\n Try No." + this.count);
                create_object(servername,portnumber);
            }
        }

        return Request_response.split("new entry"); // Returns the array of string with each index containing new entry
    }
    public static void main(String[] args) {

        String servername = args[0].split(":")[0]; // Reads in the server name from terminal
        String portnumber = args[0].split(":")[1]; // Reads in the port number from terminal

        GETClient obj = new GETClient(); // Creates an object of this class
        
        try {

                    int end = -1;
                    int feed_no;
                    feed whole_feed_temp = new feed();

                    // Initialising xstream library and configuring it such that it produces right result
                    XStream xstream = new XStream(new StaxDriver());
                    xstream.setupDefaultSecurity(xstream);
                    Class<?>[] classes = new Class[] { feed.class, _entry.class, author_class.class};
                    xstream.allowTypes(classes);
                    
                    
                    String[] aggregated_feed = obj.create_object(servername,portnumber); // Storing the array of string into a variable

                    // We are only going to display most recent 20 feeds 

                    if (aggregated_feed.length > 20) { // if there are more than 20 enteries then we will define end as aggregated_feed.length - 20
                        end = aggregated_feed.length - 20;
                    }
                    
                    for (int i = aggregated_feed.length-1; i > end; i--) { // We will start the for loop from back because we want to display the most recent feed first
                        whole_feed_temp = (feed)xstream.fromXML(aggregated_feed[i]); // Converts each feed to object
                        feed_no = i + 1;
                        System.out.println("FEED NO." + feed_no + "\n");
                        whole_feed_temp.print();                                       // Calling the object print statement
                    }

            

        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}
