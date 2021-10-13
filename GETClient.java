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
import java.util.Date;


public class GETClient {

    private int count = 0;
    private LamportClock clock = new LamportClock(0);

    public String[] create_object(String servername, String portnumber) {

        String Request_response = new String();
        String Request_response_and_lamport = new String();
        this.count += 1;
        
        try{

            Socket ss = new Socket(servername, Integer.parseInt(portnumber));

            DataInputStream input_from_server = new DataInputStream(ss.getInputStream());
            DataOutputStream output_to_server = new DataOutputStream(ss.getOutputStream());

            this.clock.increment_clock();
            String send = "GET"+"LamportClock:"+ this.clock.get_LamportClock();
            output_to_server.writeUTF(send);
            

            Request_response_and_lamport = input_from_server.readUTF();
            Request_response = Request_response_and_lamport.split("LamportClock:")[0];
            this.clock.tick(Integer.parseInt(Request_response_and_lamport.split("LamportClock:")[1]));

            input_from_server.close();
            output_to_server.close();
            ss.close();
            

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();

            if(this.count <= 3) {
                System.out.println("Retrying to connect with the Server\n\n Try No." + this.count);
                create_object(servername,portnumber);
            }
        }

        return Request_response.split("new entry");
    }
    public static void main(String[] args) {

        String servername = args[0].split(":")[0];
        String portnumber = args[0].split(":")[1];

        GETClient obj = new GETClient();
        
        try {

                    int end = -1;
                    int feed_no;
                    feed whole_feed_temp = new feed();

                    XStream xstream = new XStream(new StaxDriver());
                    xstream.setupDefaultSecurity(xstream);
                    Class<?>[] classes = new Class[] { feed.class, _entry.class, author_class.class};
                    xstream.allowTypes(classes);
                    
                    
                    String[] aggregated_feed = obj.create_object(servername,portnumber);

                    if (aggregated_feed.length > 20) {
                        end = aggregated_feed.length - 20;
                    }
                    
                    for (int i = aggregated_feed.length-1; i > end; i--) {
                        whole_feed_temp = (feed)xstream.fromXML(aggregated_feed[i]);
                        feed_no = i + 1;
                        System.out.println("FEED NO." + feed_no + "\n");
                        whole_feed_temp.print();
                    }

            

        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}
