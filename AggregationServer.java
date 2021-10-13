import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;



public class AggregationServer {

    LamportClock clock = new LamportClock(0);
    Queue<Socket> request = new LinkedList<>();


    public static void main(String[] args) throws IOException {

        AggregationServer obj = new AggregationServer();
        
        int firstArg = 4567;
        if (args.length > 0) {
            try {
                firstArg = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Argument" + args[0] + " must be an integer.");
                System.exit(1);
            }
        }
        ServerSocket s1 = new ServerSocket(firstArg);
        System.out.println("Server Started on port " + firstArg);

        
        while(true){

                Socket incoming_request = null;//listening on the port
                Socket s = null;

            try{
                incoming_request = s1.accept(); // accepting the request
                obj.request.offer(incoming_request);
                s = obj.request.poll();

                System.out.println(obj.request);

                System.out.println("New connection!!!");

                DataInputStream input_from_client = new DataInputStream(s.getInputStream());// Data from the client
                DataOutputStream output_to_client = new DataOutputStream(s.getOutputStream());

                String Incoming_str_and_lamport = input_from_client.readUTF();
                String Incoming_str = Incoming_str_and_lamport.split("LamportClock:")[0];
                obj.clock.tick(Integer.parseInt(Incoming_str_and_lamport.split("LamportClock:")[1]));

                System.out.println(Incoming_str);

                // System.out.println(Incoming_str.substring(0, 3));

                if(Incoming_str.contains("PUT")) {


                    Boolean flag = false;
    
                    if (!Incoming_str.contains("<?xml version=\"1.0\"")) {

                        flag = true;

                        System.out.println("testing 204");
    
                        obj.clock.increment_clock();
                        output_to_client.writeUTF("ERROR" + "LamportClock:"+ obj.clock.get_LamportClock());
    
                        obj.clock.increment_clock();
                        output_to_client.writeUTF("204" + "LamportClock:"+ obj.clock.get_LamportClock());
    
                        System.out.println("HELLOOOOO");
    
                        s.close();
                        input_from_client.close();
                        output_to_client.close();
                    } else {
    
                        try {
                            System.out.println("testing 500");
                            String xml = Incoming_str.split("\n\n")[1];
                            XStream xstream = new XStream(new StaxDriver());
                            feed whole_feed_temp = (feed)xstream.fromXML(xml);

                        } catch (Exception e) {
                            flag = true;
                            e.printStackTrace();
                            obj.clock.increment_clock();
                            output_to_client.writeUTF("ERROR" + "LamportClock:"+ obj.clock.get_LamportClock());

                            obj.clock.increment_clock();
                            output_to_client.writeUTF("500 - Internal server error." + "LamportClock:"+ obj.clock.get_LamportClock());

                            s.close();
                            input_from_client.close();
                            output_to_client.close();
                        }
                    }

                    if (flag == false) {
                    System.out.println("Creating new thread for the Content Server!!");
                    Thread new_thread = new ThreadCreation_Put(s,input_from_client,output_to_client,Incoming_str,obj);
            
                    new_thread.start();
                    }

                }else if(Incoming_str.contains("GET")) {

                    System.out.println("Creating new thread for the client!!");
                    Thread new_thread = new ThreadCreation_Get(s,input_from_client,output_to_client,Incoming_str,obj);
                    new_thread.start();

                }else {

                    System.out.println("testing 400");
                    obj.clock.increment_clock();
                    output_to_client.writeUTF("ERROR" + "LamportClock:"+ obj.clock.get_LamportClock());

                    obj.clock.increment_clock();
                    output_to_client.writeUTF("400" + "LamportClock:"+ obj.clock.get_LamportClock());

                    s.close();
                    input_from_client.close();
                    output_to_client.close();

                }
                
            }
            catch (Exception e) {
                s.close();
                incoming_request.close();
                e.printStackTrace();
            }
        }
    }
}

class ThreadCreation_Put extends Thread {

    final DataInputStream input_from_contentserver;
    final DataOutputStream output_to_contentserver;
    final Socket s;
    private String data;
    private String uniqueCSid;
    private AggregationServer obj = new AggregationServer();

    public ThreadCreation_Put(Socket s, DataInputStream input_from_contentserver, DataOutputStream output_to_contentserver, String Incoming_str, AggregationServer obj) {
        this.input_from_contentserver = input_from_contentserver;
        this.output_to_contentserver = output_to_contentserver;
        this.s = s;
        this.data = Incoming_str;
        this.uniqueCSid = "";
        this.obj = obj;
    }

    @Override
    public void run() {

            try {

                if (this.data.contains("ATOMClient/-1")) {
                    
                    System.out.println(data);
                    this.uniqueCSid = UUID.randomUUID().toString();

                    obj.clock.increment_clock();
                    output_to_contentserver.writeUTF(this.uniqueCSid + "LamportClock:"+ obj.clock.get_LamportClock());

                    this.data = "PUT /atom.xml HTTP/1.1\nUser-Agent: ATOMClient/" + uniqueCSid + this.data.split("ATOMClient/-1")[1];

                    FileWriter myWriter = new FileWriter("put_content.txt",true);
                    myWriter.append("\n" + this.data + "\n");
                    myWriter.close();
                    
                    obj.clock.increment_clock();
                    output_to_contentserver.writeUTF("201 - HTTP_CREATED" + "LamportClock:"+ obj.clock.get_LamportClock());

                } else {
                    Boolean flag = false;
                    System.out.println(data);

                    obj.clock.increment_clock();
                    output_to_contentserver.writeUTF("ID Present" + "LamportClock:"+ obj.clock.get_LamportClock());

                    this.uniqueCSid = data.split("ATOMClient/")[1].split("\nContent-Type")[0];
                    System.out.println(uniqueCSid);
                    
                    BufferedReader read= new BufferedReader(new FileReader("put_content.txt"));
                    ArrayList<String> list = new ArrayList<String>();

                    String dataRow = read.readLine(); 
                    while (dataRow != null){
                        // System.out.println(dataRow);
                        list.add(dataRow);
                        dataRow = read.readLine(); 
                    }

                    FileWriter writer = new FileWriter("put_content.txt");

                    for (int i = 0; i < list.size(); i++){

                        if(i < list.size()) {
                            System.out.println(list.get(i));
                            System.out.println(i);
                            if (list.get(i).contains("ATOMClient/") ) {
                                if (list.get(i).split("ATOMClient/")[1].equals(this.uniqueCSid)) {
                                    flag = true;
                                    writer.append(list.get(i));
                                    writer.append("\nContent-Type: ATOM syndication\n");
                                    String xml_data = "<?xml version=\"1.0" + data.split("<?xml version=\"1.0")[1];
                                    writer.append("Content-Length: " + xml_data.length() + "\n\n");
                                    writer.append(xml_data + "\n");
                                    i = i + 4;
                                    continue;
                                }
                            }
                            writer.append(list.get(i));
                            writer.append(System.getProperty("line.separator"));
                        }
                    }
                    writer.flush();
                    writer.close();
                    read.close();

                    if (flag == true) {
                        
                        obj.clock.increment_clock();
                        output_to_contentserver.writeUTF("200" + "LamportClock:"+ obj.clock.get_LamportClock());
                    }
                    else {
                        FileWriter myWriter = new FileWriter("put_content.txt",true);
                        myWriter.append("\n" + this.data);
                        myWriter.close();
                        
                        obj.clock.increment_clock();
                        output_to_contentserver.writeUTF("201 - HTTP_CREATED" + "LamportClock:"+ obj.clock.get_LamportClock());
                    }

                }

                    while (true) {
                        
                        obj.clock.increment_clock();
                        output_to_contentserver.writeUTF("Alive?" + "LamportClock:"+ obj.clock.get_LamportClock());

                        String heartbeat_and_lamport = input_from_contentserver.readUTF();
                        String heartbeat = heartbeat_and_lamport.split("LamportClock:")[0];
                        obj.clock.tick(Integer.parseInt(heartbeat_and_lamport.split("LamportClock:")[1]));

                        if (heartbeat.equals("Yes")) {
                            System.out.println(this.uniqueCSid + " IS ALIVEE!!");
                        }

                        try {
                            Thread.sleep(12000);
                        }
                        catch(InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

                    }           

            }catch (IOException e) {

                try{

                    BufferedReader read= new BufferedReader(new FileReader("put_content.txt"));
                    ArrayList<String> list = new ArrayList<String>();

                    String dataRow = read.readLine(); 
                    while (dataRow != null){
                        // System.out.println(dataRow);
                        list.add(dataRow);
                        dataRow = read.readLine(); 
                    }

                    FileWriter writer = new FileWriter("put_content.txt");

                    for (int i = 0; i < list.size(); i++){
                        if (i<list.size()-1) {
                            System.out.println(list.get(i));
                            System.out.println(i);
                            if (list.get(i+1).contains("ATOMClient/") ) {
                                if (list.get(i+1).split("ATOMClient/")[1].equals(this.uniqueCSid)) {
                                    i = i + 6;
                                    continue;
                                }
                            }
                        }
                            writer.append(list.get(i));
                            writer.append(System.getProperty("line.separator"));
                    }
                    writer.flush();
                    writer.close();
                    read.close();

                    this.input_from_contentserver.close();
                    this.output_to_contentserver.close();
                    this.s.close();
                    System.out.println("Content Removed since Content Server Didn't Respond");
                }
                catch(IOException d){
                    d.printStackTrace();
                }
            }

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

class ThreadCreation_Get extends Thread {

    final DataInputStream input_from_client;
    final DataOutputStream output_to_client;
    final Socket s;
    final String data;
    private AggregationServer obj = new AggregationServer();

    public ThreadCreation_Get(Socket s, DataInputStream input_from_client, DataOutputStream output_to_client, String Incoming_str, AggregationServer obj){
        this.input_from_client = input_from_client;
        this.output_to_client = output_to_client;
        this.s = s;
        this.data = Incoming_str;
        this.obj = obj;
    }


    @Override
    public void run() {
            String get_request = "";
            try{

                File myObj = new File("put_content.txt");
                Scanner myReader = new Scanner(myObj);
                while (myReader.hasNextLine()) {

                    String temp = myReader.nextLine();

                    if (temp.contains("<?xml version=\"1.0\" ?><feed xml:lang=\"en-US\" xmlns=\"http://www.w3.org/2005/Atom\">")) {
                        get_request += temp;
                        get_request += "new entry";
                    }

                }
                myReader.close();

                obj.clock.increment_clock();
                output_to_client.writeUTF(get_request + "LamportClock:"+ obj.clock.get_LamportClock());
                

            }catch (IOException e) {
                e.printStackTrace();
            }

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
