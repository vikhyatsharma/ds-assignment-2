import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import com.thoughtworks.xstream.XStream; //Importing xstream libary which help in convert xml to obj and vice versa
import com.thoughtworks.xstream.io.xml.StaxDriver;

// Main class called AggregationServer
public class AggregationServer {

    LamportClock clock = new LamportClock(0);    //Instance of lamport clock for the AggregationServer
    Queue<Socket> request = new LinkedList<>(); // Queue that will store the incoming request

    public static void main(String[] args) throws IOException {

        AggregationServer obj = new AggregationServer(); // Creating object of this class to access the lamport clock and request queue
        
        int firstArg = 4567;   //default port number
        if (args.length > 0) {
            try {
                firstArg = Integer.parseInt(args[0]); //If port number specified then use that instead
            } catch (NumberFormatException e) {
                System.err.println("Argument" + args[0] + " must be an integer.");
                System.exit(1);
            }
        }

        ServerSocket s1 = new ServerSocket(firstArg); //Starting server socket on the given port
        System.out.println("Server Started on port " + firstArg);

        while(true){ //Infinite while loop so that server is always listening for connections

                Socket incoming_request = null;
                Socket s = null; //Initialising the socket

            try{

                //The request is first come first serve

                incoming_request = s1.accept(); // accepting the request
                obj.request.offer(incoming_request); // Put the request in the queue
                s = obj.request.poll(); //Take the top most request

                System.out.println("New connection!!!");

                DataInputStream input_from_client = new DataInputStream(s.getInputStream()); // Establishing connection to send and recieve data
                DataOutputStream output_to_client = new DataOutputStream(s.getOutputStream());

                String Incoming_str_and_lamport = input_from_client.readUTF(); // Reading the incoming string
                String Incoming_str = Incoming_str_and_lamport.split("LamportClock:")[0]; //Seperating the lamport clock from the request
                obj.clock.tick(Integer.parseInt(Incoming_str_and_lamport.split("LamportClock:")[1])); // Updating the Lamport clock using tick function

                System.out.println(Incoming_str);

                if(Incoming_str.contains("PUT")) { //If the connection is PUT the string will have "PUT"


                    Boolean flag = false; //To check if the request is valid, if in end its false that mean we can create thread if not then we will close the connection
    
                    if (!Incoming_str.contains("<?xml version=\"1.0\"")) { // If the request doesn't contain this string that means there is no xml present and we will return code 204

                        flag = true; //There is an issue with the request

                        System.out.println("testing 204");
                        
                        obj.clock.increment_clock(); 
                        output_to_client.writeUTF("ERROR" + "LamportClock:"+ obj.clock.get_LamportClock()); // Updating the lamport clock and sending that an error occurred
    
                        obj.clock.increment_clock();
                        output_to_client.writeUTF("204" + "LamportClock:"+ obj.clock.get_LamportClock()); //updating the lamport clock and sending the error code
    
                        s.close(); // Closing the connection
                        input_from_client.close();
                        output_to_client.close();

                    } else {  // If string does contain xml we are gonna check if it is valid or not
    
                        try {
                            System.out.println("testing 500"); // Testing the code for code 500
                            String xml = Incoming_str.split("\n\n")[1]; // Getting the xml from the string
                            XStream xstream = new XStream(new StaxDriver());
                            feed whole_feed_temp = (feed)xstream.fromXML(xml);  // converting it to an object

                        } catch (Exception e) {     // If there is any error while converting that means xml is malformed and we need to send code 500

                            flag = true;   // Xml is malformed so we will turn the flag to true
                            e.printStackTrace();

                            obj.clock.increment_clock();
                            output_to_client.writeUTF("ERROR" + "LamportClock:"+ obj.clock.get_LamportClock()); // Updating the lamport clock and sending that an error occurred

                            obj.clock.increment_clock();
                            output_to_client.writeUTF("500 - Internal server error." + "LamportClock:"+ obj.clock.get_LamportClock()); // Updating the lamport clock and sending the error code

                            s.close();  // Closing the connection
                            input_from_client.close();
                            output_to_client.close();
                        }
                    }

                    if (flag == false) {    // If flag is still false that means there is no problem with the request and we can go ahead and create a thread for the request

                        System.out.println("Creating new thread for the Content Server!!");
                        Thread new_thread = new ThreadCreation_Put(s,input_from_client,output_to_client,Incoming_str,obj); // Creating thread for the put request
                
                        new_thread.start(); // Starting the thread

                    }

                }else if(Incoming_str.contains("GET")) {    // If the request is from the client it will have "GET" in the string and we can go ahead and create thread for it

                    System.out.println("Creating new thread for the client!!");
                    Thread new_thread = new ThreadCreation_Get(s,input_from_client,output_to_client,Incoming_str,obj); // Creating thread for the get client
                    new_thread.start(); //Starting the thread

                }else { // If the string does not contain "PUT" or "GET" then we will return the error code 400

                    System.out.println("testing 400");

                    obj.clock.increment_clock();
                    output_to_client.writeUTF("ERROR" + "LamportClock:"+ obj.clock.get_LamportClock()); // Updating the lamport clock and sending that an error occurred

                    obj.clock.increment_clock();
                    output_to_client.writeUTF("400" + "LamportClock:"+ obj.clock.get_LamportClock()); // Updating the lamport clock and sending the error code

                    s.close(); // Closing the thread
                    input_from_client.close();
                    output_to_client.close();

                }
                
            }
            catch (Exception e) { // If any error occured in the process
                s.close();
                incoming_request.close();
                e.printStackTrace();
            }
        }
    }
}

//Class for the request coming from the content server

class ThreadCreation_Put extends Thread {

    final DataInputStream input_from_contentserver; // Helps in recieving data from contentserver
    final DataOutputStream output_to_contentserver; // Helps in sending data to contentserver
    final Socket s; // Socket between AS and CS
    private String data; // data in put request goes here
    private String uniqueCSid; // We generate unique id for each content server which gets stored here
    private AggregationServer obj = new AggregationServer(); // Obj of the AggregationServer to access lamport clock inside this class

    // Constructor initialises everything
    public ThreadCreation_Put(Socket s, DataInputStream input_from_contentserver, DataOutputStream output_to_contentserver, String Incoming_str, AggregationServer obj) {
        this.input_from_contentserver = input_from_contentserver;
        this.output_to_contentserver = output_to_contentserver;
        this.s = s;
        this.data = Incoming_str;
        this.uniqueCSid = "";
        this.obj = obj;
    }

    // Running the thread
    @Override
    public void run() {

            try {

                if (this.data.contains("ATOMClient/-1")) { // If the content server has never connected with this server in the past it will have "ATOMClient/-1" in the string, if it has it will have some id instead of -1
                    
                    System.out.println(data); // Printing the data
                    this.uniqueCSid = UUID.randomUUID().toString(); // Generating a unique id for the content server

                    obj.clock.increment_clock();
                    output_to_contentserver.writeUTF(this.uniqueCSid + "LamportClock:"+ obj.clock.get_LamportClock()); // Updating the lamport clock and sending the unique id to content server so that it can store it in the file

                    this.data = "PUT /atom.xml HTTP/1.1\nUser-Agent: ATOMClient/" + uniqueCSid + this.data.split("ATOMClient/-1")[1]; // replacing -1 with the id generated

                    FileWriter myWriter = new FileWriter("All_content_server_data.txt",true); // appending the data came from content server to a file called All_content_server_data.txt which will store all the data
                    myWriter.append("\n" + this.data + "\n");
                    myWriter.close();
                    
                    obj.clock.increment_clock();
                    output_to_contentserver.writeUTF("201 - HTTP_CREATED" + "LamportClock:"+ obj.clock.get_LamportClock()); //Updating the lamport clock and sending the 201 code

                } else { // If an id is already present in the put request we will check if that id is valid or not. If it is we will update the content of that server if not we will append the new data
                    
                    Boolean flag = false; // TO check if the id is valid
                    System.out.println(data);

                    obj.clock.increment_clock();
                    output_to_contentserver.writeUTF("ID Present" + "LamportClock:"+ obj.clock.get_LamportClock()); //updating Lamport clock and telling content server that an id is present

                    this.uniqueCSid = data.split("ATOMClient/")[1].split("\nContent-Type")[0]; // Getting the unique id
                    System.out.println(uniqueCSid);
                    
                    BufferedReader read= new BufferedReader(new FileReader("All_content_server_data.txt")); // opening the file
                    ArrayList<String> list = new ArrayList<String>();

                    String dataRow = read.readLine();  // Reading everything line by line from the file
                    while (dataRow != null){
                        list.add(dataRow);      // storing it in an array
                        dataRow = read.readLine(); 
                    }

                    FileWriter writer = new FileWriter("All_content_server_data.txt"); // opening the file to write

                    for (int i = 0; i < list.size(); i++){ // Going over the entire array

                        if(i < list.size()) {
                            if (list.get(i).contains("ATOMClient/") ) {
                                if (list.get(i).split("ATOMClient/")[1].equals(this.uniqueCSid)) { // If we find the feed with the same id we will update the content
                                    flag = true;
                                    writer.append(list.get(i));
                                    writer.append("\nContent-Type: ATOM syndication\n");
                                    String xml_data = "<?xml version=\"1.0" + data.split("<?xml version=\"1.0")[1];
                                    writer.append("Content-Length: " + xml_data.length() + "\n\n");
                                    writer.append(xml_data + "\n"); // Updating the content
                                    i = i + 4;
                                    continue;
                                }
                            }
                            writer.append(list.get(i));     // Adding the rest of content back
                            writer.append(System.getProperty("line.separator"));
                        }
                    }
                    // closing all the writer
                    writer.flush();
                    writer.close();
                    read.close();

                    if (flag == true) { // This means id was valid and we updated the feed
                        
                        obj.clock.increment_clock();
                        output_to_contentserver.writeUTF("200" + "LamportClock:"+ obj.clock.get_LamportClock()); // Updating the lamport clock and sending 200 code
                    }
                    else {  // If id is not present in the file we will simply append the incoming data
                        FileWriter myWriter = new FileWriter("All_content_server_data.txt",true);
                        myWriter.append("\n" + this.data);
                        myWriter.close();
                        
                        obj.clock.increment_clock();
                        output_to_contentserver.writeUTF("201 - HTTP_CREATED" + "LamportClock:"+ obj.clock.get_LamportClock()); // updating the lamport clock and sending 201 code
                    }

                }

                // HEARTBEAT

                    while (true) { // Infinite while loop which ask content server every 12 second if it is alive or not
                        
                        obj.clock.increment_clock();
                        output_to_contentserver.writeUTF("Alive?" + "LamportClock:"+ obj.clock.get_LamportClock()); // Updating lamport clock and asking content server if it is alive

                        String heartbeat_and_lamport = input_from_contentserver.readUTF(); // Reading from the client
                        String heartbeat = heartbeat_and_lamport.split("LamportClock:")[0]; // Removing the lamport clock from the incoming request
                        obj.clock.tick(Integer.parseInt(heartbeat_and_lamport.split("LamportClock:")[1]));

                        if (heartbeat.equals("Yes")) {  // If content server is alive
                            System.out.println(this.uniqueCSid + " IS ALIVEE!!"); // Print its alive
                        }

                        try {  
                            Thread.sleep(12000);        // Sleep for 12 second and repeat
                        }
                        catch(InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

                    }           

            }catch (IOException e) {  // If content server dies this will remove content of that server

                try{

                    BufferedReader read= new BufferedReader(new FileReader("All_content_server_data.txt")); 
                    ArrayList<String> list = new ArrayList<String>();

                    String dataRow = read.readLine(); // Reading the content from the file
                    while (dataRow != null){
                        list.add(dataRow);  // Storing it in an array
                        dataRow = read.readLine(); 
                    }

                    FileWriter writer = new FileWriter("All_content_server_data.txt"); // opening file to write

                    for (int i = 0; i < list.size(); i++){
                        if (i<list.size()-1) {
                            if (list.get(i+1).contains("ATOMClient/") ) {
                                if (list.get(i+1).split("ATOMClient/")[1].equals(this.uniqueCSid)) { // Identifying the feed with the id and removing the content
                                    i = i + 6;
                                    continue;
                                }
                            }
                        }
                            writer.append(list.get(i)); // Writing the rest of the file 
                            writer.append(System.getProperty("line.separator"));
                    }

                    // Closing all file
                    writer.flush();
                    writer.close();
                    read.close();

                    // Closing the connection
                    this.input_from_contentserver.close();
                    this.output_to_contentserver.close();
                    this.s.close();
                    System.out.println("Content Removed since Content Server Didn't Respond");
                }
                catch(IOException d){
                    d.printStackTrace();
                }
            }

        // After everything close connection with the content server
        try
        {
            this.input_from_contentserver.close();
            this.output_to_contentserver.close();
            this.s.close();
            
        } catch(IOException e) {
            e.printStackTrace();
        }
       
    }
}

//Class for the request coming from the Client

class ThreadCreation_Get extends Thread {

    final DataInputStream input_from_client; // Helps in recieving data from client
    final DataOutputStream output_to_client; // Helps in sending data to client
    final Socket s; // Socket between AS and client
    final String data; // data in get request goes here
    private AggregationServer obj = new AggregationServer();// Obj of the AggregationServer to access lamport clock inside this class

    // Constructor initialises everything
    public ThreadCreation_Get(Socket s, DataInputStream input_from_client, DataOutputStream output_to_client, String Incoming_str, AggregationServer obj){
        this.input_from_client = input_from_client;
        this.output_to_client = output_to_client;
        this.s = s;
        this.data = Incoming_str;
        this.obj = obj;
    }

    // Running the thread
    @Override
    public void run() {
            String get_request = ""; // Final string that would be send to the client
            try{

                File myObj = new File("All_content_server_data.txt"); // Opening the file to read in the data
                Scanner myReader = new Scanner(myObj);
                while (myReader.hasNextLine()) { // Reading line by line

                    String temp = myReader.nextLine();

                    if (temp.contains("<?xml version=\"1.0\" ?><feed xml:lang=\"en-US\" xmlns=\"http://www.w3.org/2005/Atom\">")) { // If the line is xml add it in string
                        get_request += temp; // Adding xml to the string
                        get_request += "new entry"; // Adding "new entry" after every new entry
                    }

                }
                myReader.close();

                obj.clock.increment_clock();
                output_to_client.writeUTF(get_request + "LamportClock:"+ obj.clock.get_LamportClock()); // updating the lamport clock and sending the request
                

            }catch (IOException e) {
                e.printStackTrace();
            }

        // Closing the connection with the client after sending the file

        try
        {
            this.input_from_client.close();
            this.output_to_client.close();
            this.s.close();
            
        } catch(IOException e) {
            e.printStackTrace();
        }
       
    }
}
