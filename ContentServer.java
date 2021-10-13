import java.util.*;
import java.io.*;
import java.net.*;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;
// import java.lang.Objectcom.thoughtworks.xstream.security.AnyTypePermission;
// import javax.xml.bind.JAXBContext;
// import javax.xml.bind.JAXBException;
// import javax.xml.bind.Marshaller;



public class ContentServer{

    private LamportClock clock = new LamportClock(0);
    private String uniqueCSid = "-1";    
    private boolean feed = true;
    private int counter = 0;


    public String get_uniqueCSid() {
        return this.uniqueCSid;
    }

    public Boolean get_feed() {
        return this.feed;
    }

    public void set_uniqueCSid(String input) {
        this.uniqueCSid = input;
    }

    public void set_feed(Boolean input) {
        this.feed = input;
    }

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

        return whole_feed;

    }

    public void Create_connection(String xml, String servername, String portnumber ) {

        this.counter += 1;

        try {
            
        Socket ss = new Socket(servername, Integer.parseInt(portnumber));
        DataOutputStream output_to_server = new DataOutputStream(ss.getOutputStream());
        DataInputStream input_from_client = new DataInputStream(ss.getInputStream());
        
        this.clock.increment_clock();
        output_to_server.writeUTF(xml + "LamportClock:"+ this.clock.get_LamportClock());

        String id_and_lamport = input_from_client.readUTF();
        String id = id_and_lamport.split("LamportClock:")[0];
        this.clock.tick(Integer.parseInt(id_and_lamport.split("LamportClock:")[1]));

        uniqueCSid = id;

        if ( !id.equals("ID Present") && !id.equals("ERROR")) {

            BufferedReader read= new BufferedReader(new FileReader("content_2.txt"));
            ArrayList<String> list = new ArrayList<String>();

            String dataRow = read.readLine(); 
            while (dataRow != null){
                list.add(dataRow);
                dataRow = read.readLine(); 
            }

            FileWriter writer = new FileWriter("content_2.txt"); //same as your file name above so that it will replace it
            writer.append("uniqueCSid:" + id);

            for (int i = 0; i < list.size(); i++){
                writer.append(System.getProperty("line.separator"));
                writer.append(list.get(i));
            }
            writer.flush();
            writer.close();
            read.close();

            this.counter = 0;
        }

        String lamport_and_responseID = input_from_client.readUTF();
        String responseID = lamport_and_responseID.split("LamportClock:")[0];
        this.clock.tick(Integer.parseInt(lamport_and_responseID.split("LamportClock:")[1]));

        System.out.println(responseID);

        if (responseID.equals("200") || responseID.equals("201 - HTTP_CREATED")) {

            while (true) {

                
                String heartbeat_and_lamport = input_from_client.readUTF();
                String heartbeat = heartbeat_and_lamport.split("LamportClock:")[0];
                this.clock.tick(Integer.parseInt(heartbeat_and_lamport.split("LamportClock:")[1]));

                if (heartbeat.equals("Alive?")) {
                    this.clock.increment_clock();
                    output_to_server.writeUTF("Yes"+"LamportClock:"+ this.clock.get_LamportClock());
                }

            }

        }

        ss.close();
        output_to_server.close();

        }
        catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();

            if(this.counter <= 3) {
                System.out.println("Retrying to connect with the Server\n\n Try No." + this.counter);
                Create_connection(xml,servername,portnumber);
            }
        }
    }

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

                String servername = args[0].split(":")[0];
                String portnumber = args[0].split(":")[1];
                String location = args[1];
                
                ContentServer obj = new ContentServer();

                feed whole_feed = obj.create_object(location);

                String xml = "PUT /atom.xml HTTP/1.1\nUser-Agent: ATOMClient/" + obj.get_uniqueCSid() + "\nContent-Type: ATOM syndication\nContent-Length: ";

                XStream xstream = new XStream(new StaxDriver());
                xstream.setupDefaultSecurity(xstream);
                Class<?>[] classes = new Class[] { feed.class, _entry.class, author_class.class};
                xstream.allowTypes(classes);
                xstream.useAttributeFor(feed.class, "attribute2");
                xstream.aliasField("xmlns", feed.class, "attribute2");
                xstream.useAttributeFor(feed.class, "attribute1");
                xstream.aliasField("xml:lang", feed.class, "attribute1");
                String xml_temp = xstream.toXML(whole_feed);
                // System.out.println(formatXml(xml_temp));

                // feed whole_feed_temp = (feed)xstream.fromXML(xml_temp);
                // whole_feed_temp.print();

                String temp_str = xml_temp.length() + "\n\n";
                xml += temp_str + xml_temp ;
                System.out.println(xml.substring(0,3));
                System.out.println(xml);

                obj.Create_connection(xml,servername,portnumber);
    }

}
