import java.util.*;
import java.io.*;
import java.net.*;
import com.thoughtworks.xstream.XStream; // Xstream library to convert xml to obj and vice versa
import com.thoughtworks.xstream.io.xml.StaxDriver;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;

// Main class content Server

public class ContentServer{

    private LamportClock clock = new LamportClock(0); // Initialising lamport clock for this class
    private String uniqueCSid = "-1"; // Initialising id as -1   
    private boolean feed = true;
    private int counter = 0; // Initialising counter as 0


    public String get_uniqueCSid() { // Function to get the id
        return this.uniqueCSid;
    }

    public Boolean get_feed() {
        return this.feed;
    }

    public void set_uniqueCSid(String input) { // Function to set the unique id
        this.uniqueCSid = input;
    }

    public void set_feed(Boolean input) { // Function to set the feed
        this.feed = input;
    }

    // This function reads the file and converts it into single class object so that we can use the library to convert that object into xml and send it to aggregation Server

    public feed create_object(String location) {

        feed whole_feed = new feed(); // This object will store the whole feed
        author_class author = new author_class(); // This stores the author name
        ArrayList<_entry> entry_obj = new ArrayList<_entry>(); // Stores all the entry object in arraylist
        ArrayList<String> string_list = new ArrayList<String>(); // Stores the information about the feed
        ArrayList<String> entry_string_list = new ArrayList<String>();// Stores the information about each entry

            try {
                
                String temp,temp1,temp2 = ""; // Temporary variables to get each line from the .txt file
                File myObj = new File(location); // Open the file
                Scanner myReader = new Scanner(myObj);
                int count_feed = 0; // To check if the feed has title,link and id, if they are all present count_feed will be equal to 3 at the end

                while (myReader.hasNextLine()) { //Running the loop till the end of file
                
                    temp = myReader.nextLine(); // Getting the next line

                    if (temp.contains("uniqueCSid")) {
                        uniqueCSid = temp.split(":")[1];
                        temp = myReader.nextLine();
                    }
                    
                    temp1 = temp.split(":")[0]; // Storing the title in temp1 

                    if(temp.split(":").length > 1) {
                        int size = temp.split(temp1 + ":").length;
                        temp2 = temp.split(temp1 + ":")[size-1];    // Storing the data in temp2
                    }

                    //Check condition for each case and adding the content to the arraylist
                    if (temp1.equals("title")) {

                        if (temp2.length() == 0) { // If temp2 is empty we will break
                            feed = false;
                            break;
                        }                           
                        else 
                            string_list.add(temp2);
                        count_feed++; // Updating the count

                    } else if (temp1.equals("subtitle")) {

                        string_list.add(temp2);

                    } else if (temp1.equals("link")) {

                        if (temp2.length() == 0) { // If temp2 is empty we will break
                            feed = false;
                            break;
                        }  
                        else 
                            string_list.add(temp2);
                        count_feed++;   // Updating the count

                    } else if (temp1.equals("updated")) {

                        string_list.add(temp2);

                    } else if (temp1.equals("id")) {

                        if (temp2.length() == 0 || temp2.equals("urn::uu")) { // If temp2 is empty we will break
                            feed = false;
                            break;
                        }  
                        else 
                            string_list.add(temp2);
                        count_feed++; // Updating the count

                    } else if (temp1.equals("author")){

                        author = new author_class(temp2);

                    } else if (temp1.equals("entry")){ // If temp1 is entry we will start another while loop inside to get all the entries in one arraylist
                        
                        _entry temp_obj = new _entry();

                        boolean entry = true; // To check if the there is any data in title, link and id
                        int count = 0; // To check if title, link and id are even present in the file

                        while(myReader.hasNextLine()) {

                            temp = myReader.nextLine();
                            temp1 = temp.split(":")[0]; // Temp1 stores the title
                            temp2 = "";

                            if(temp.split(":").length > 1) {
                                int size = temp.split(temp1 + ":").length;
                                temp2 = temp.split(temp1 + ":")[size-1]; // Temp2 takes the data
                            }

                            //checking each tag and adding the data in the arraylist
                            if (temp1.equals("title")) {

                                if (temp2.length() == 0){ // If no title we will discard this entry
                                    entry = false;      
                                }                    
                                else
                                    entry_string_list.add(temp2);
                                count++;

                            }else if (temp1.equals("link")) { // If no link we will discard this entry

                                if (temp2.length() == 0)
                                    entry = false;                           
                                else
                                    entry_string_list.add(temp2);
                                count++;

                            } else if (temp1.equals("id")) { // If no id we will discard this entry

                                if (temp2.length() == 0 || temp2.equals("urn:uu"))
                                    entry = false;                           
                                else
                                    entry_string_list.add(temp2);
                                count++;

                            } else if (temp1.equals("updated")) {

                                entry_string_list.add(temp2);

                            }else if (temp1.equals("summary")) {

                                entry_string_list.add(temp2);

                            }else if (temp1.equals("entry")){ // If we hit another entry then we will add the data into the arraylist and repeat

                                if(entry == true && count == 3) { // Entry needs to be true and counts need to be 3

                                    temp_obj = new _entry(entry_string_list.get(0),entry_string_list.get(1),entry_string_list.get(2),entry_string_list.get(3),entry_string_list.get(4));
                                    entry_obj.add(temp_obj);  

                                }         

                                // Reset the arraylist entry and count       
                                entry_string_list = new ArrayList<String>(); 
                                entry = true;
                                count = 0;
                            }
                            // If it is the end of file we will add the data in the arraylist
                            if(myReader.hasNextLine() != true && entry == true && count == 3) {

                                temp_obj = new _entry(entry_string_list.get(0),entry_string_list.get(1),entry_string_list.get(2),entry_string_list.get(3),entry_string_list.get(4));
                                entry_obj.add(temp_obj);  

                            }

                        }

                    }

                }

                myReader.close();
                // If feed is true and count_feed is equal to 3 we will create the whole feed object
                if (feed == true && count_feed == 3) {
                    whole_feed = new feed(string_list.get(0),string_list.get(1),string_list.get(2),string_list.get(3),author,string_list.get(4),entry_obj);
                }

            }
            catch (FileNotFoundException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }

        return whole_feed;  // return the final object created

    }


    // This function gets servername, portnumber and xml as parameter and creates connectoin with the server and sends the xml file

    public void Create_connection(String xml, String servername, String portnumber, String location ) {

        this.counter += 1; // Overall we will try 3 times to connect with the server, this will get updated everytime because we are doing recursive call

        try {
            
        Socket ss = new Socket(servername, Integer.parseInt(portnumber));   // Making connection with the Server
        DataOutputStream output_to_server = new DataOutputStream(ss.getOutputStream());
        DataInputStream input_from_client = new DataInputStream(ss.getInputStream());
        
        this.clock.increment_clock();
        output_to_server.writeUTF(xml + "LamportClock:"+ this.clock.get_LamportClock()); //Updating the lamport clock and sending the xml to Aggregation server

        String id_and_lamport = input_from_client.readUTF();
        String id = id_and_lamport.split("LamportClock:")[0];
        this.clock.tick(Integer.parseInt(id_and_lamport.split("LamportClock:")[1])); // Updating the lamport clock and getting the id from the Aggregation serve

        uniqueCSid = id; // Assigning the id to uniqueCSid

        if ( !id.equals("ID Present") && !id.equals("ERROR")) { // If id is real then we will append the id on top of the content.txt so that if content server dies and comes back alive we will still have the id

            BufferedReader read= new BufferedReader(new FileReader(location)); // Opening the file
            ArrayList<String> list = new ArrayList<String>();

            String dataRow = read.readLine(); // Reading it line by line
            while (dataRow != null){
                list.add(dataRow); // Adding it to the array
                dataRow = read.readLine(); 
            }

            FileWriter writer = new FileWriter(location); // Opening the file
            writer.append("uniqueCSid:" + id); // Adding the id

            for (int i = 0; i < list.size(); i++){ // For the rest of the file we will just add the data back from the array
                writer.append(System.getProperty("line.separator"));
                writer.append(list.get(i));
            }
            writer.flush();
            writer.close();
            read.close();

            this.counter = 0; // if everything goes well we will do counter = 0
        }

        String lamport_and_responseID = input_from_client.readUTF(); // Getting the data from the server
        String responseID = lamport_and_responseID.split("LamportClock:")[0];
        this.clock.tick(Integer.parseInt(lamport_and_responseID.split("LamportClock:")[1])); // Updating the lamport clock and getting the responseID

        System.out.println(responseID);

        if (responseID.equals("200") || responseID.equals("201 - HTTP_CREATED")) { // If the error code is either 200 or 201 we will enter the heartbeat Stage

            // HEARTBEAT

            while (true) { // Infinite while loop for heartbeat

                String heartbeat_and_lamport = input_from_client.readUTF();
                String heartbeat = heartbeat_and_lamport.split("LamportClock:")[0];
                this.clock.tick(Integer.parseInt(heartbeat_and_lamport.split("LamportClock:")[1])); // Reading from the Server and updating the lamport clock

                if (heartbeat.equals("Alive?")) {
                    this.clock.increment_clock();
                    output_to_server.writeUTF("Yes"+"LamportClock:"+ this.clock.get_LamportClock()); // If the server asked "alive?" We will respond with yes
                }

            }

        }

        ss.close();
        output_to_server.close();

        }
        // If an error occured then we will try 3 times before giving up

        catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();

            if(this.counter <= 3) { // If the counter is less than or equal to 3 we will recursively call this function again
                System.out.println("Retrying to connect with the Server\n\n Try No." + this.counter);
                Create_connection(xml,servername,portnumber,location);
            }
        }
    }

    // This function prints the xml in a nice readable form
    public static String formatXml(String xml) {
   
        try {
           Transformer serializer = SAXTransformerFactory.newInstance().newTransformer();
           
           serializer.setOutputProperty(OutputKeys.INDENT, "yes");
           serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
           
           Source xmlSource = new SAXSource(new InputSource(
              new ByteArrayInputStream(xml.getBytes())));
           StreamResult res =  new StreamResult(new ByteArrayOutputStream());            
           
           serializer.transform(xmlSource, res);
           
           return new String(((ByteArrayOutputStream)res.getOutputStream()).toByteArray());
           
        } catch(Exception e) {
           return xml;
        }
     }

  
    // Main function
    public static void main(String[] args){

                System.out.println(args);

                String servername = args[0].split(":")[0]; // Getting the server name as input from the terminal
                String portnumber = args[0].split(":")[1]; // Getting portnumber as input from terminal
                String location = args[1]; // Location of the content file
                
                ContentServer obj = new ContentServer(); // Create a new object of the class

                feed whole_feed = obj.create_object(location); // This function will read the file location convert it into an object and return it back

                String xml = "PUT /atom.xml HTTP/1.1\nUser-Agent: ATOMClient/" + obj.get_uniqueCSid() + "\nContent-Type: ATOM syndication\nContent-Length: "; // generating the start of the xml

                // Initialising xstream library and configuring it such that it produces right result
                XStream xstream = new XStream(new StaxDriver());
                xstream.setupDefaultSecurity(xstream);
                Class<?>[] classes = new Class[] { feed.class, _entry.class, author_class.class};
                xstream.allowTypes(classes);
                xstream.useAttributeFor(feed.class, "attribute2");
                xstream.aliasField("xmlns", feed.class, "attribute2");
                xstream.useAttributeFor(feed.class, "attribute1");
                xstream.aliasField("xml:lang", feed.class, "attribute1");

                String xml_temp = xstream.toXML(whole_feed); // Converting the object to xml

                String temp_str = xml_temp.length() + "\n\n";
                xml += temp_str + xml_temp ;    // Final xml produced
                System.out.println(xml.substring(0,3));
                System.out.println(formatXml(xml)); // Printing the final xml

                obj.Create_connection(xml,servername,portnumber,location); // Sending it to the server
    }

}
