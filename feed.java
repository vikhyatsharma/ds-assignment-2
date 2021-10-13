

import java.util.*;
// Class feed which stores the information about the feed and the list of enteries

public class feed {
    private String title;
    private String subtitle;
    private String link;
    private String updated;
    private author_class author;
    private String id;
    private String attribute1; 
    private String attribute2;
    private ArrayList<_entry> entries = new ArrayList<_entry>();

    public feed(){};

    // Constructor which takes in all the variable in parameter and initializes it
    public feed(String title,String subtitle,String link,String updated,author_class auth,String id,ArrayList<_entry> entries){
        this.title = title;
        this.subtitle = subtitle;
        this.link = link;
        this.updated = updated;
        this.author = auth;
        this.id = id;
        this.entries = entries;
        this.attribute1 = "en-US";
        this.attribute2 = "http://www.w3.org/2005/Atom";
    }

    // Print statement to output all the data in the object
    public void print(){
        System.out.println("title:" + title + "\n" + "subtitle:" + subtitle + "\n" + "link:" + link + "\n" + "updated:" + updated + "\n" + "author:" + author.get() + "\n" + "id:" + id + "\n\n");
        for(Integer i = 0; i<entries.size(); i++){
            int entry_no = i + 1;
            System.out.println("ENTRY NO." + entry_no);
            entries.get(i).print();
        }
    }

     public static void main(String[] args) {
        System.out.println("Feed Class");
    }

}

class author_class{
    private String name;

    // Constructor that takes name as in input and initialises the variable
    public author_class(String name) {
        this.name = name;
    }

    public author_class(){};

    // Getter function to get the name stored in the object
    public String get() {
        return name;
    }
}

// Entry class to store all the information about each entries
class _entry{
    private String title;
    private String link;
    private String id;
    private String updated;
    private String summary;

    public _entry(){};
    
    // Constructor that takes the value as in input and stores it in the variable
    public _entry(String title,String link,String id,String updated,String summary){
        this.title = title;
        this.link = link;
        this.updated = updated;
        this.summary = summary;
        this.id = id;
    }

    // Print method to print what data is stored in the object
    public void print(){
        System.out.println("title:" + title + "\n" + "link:" + link + "\n" + "id:" + id + "\n" + "updated:" + updated + "\n" + "summary:" +summary + "\n");
    }
}