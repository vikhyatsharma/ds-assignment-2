#!/user/bin/bash
#To run the rmiregistry
rmiregistry &
#sleep so that previous command is finished executing before running the next command
sleep 3
#To run the surver
java SorterServer &
#sleep so that previous command is finished executing before running the next command
sleep 3
#Running the client with arguments
java SorterClient 4 3 1 3 2 1 6 4 8 9 10 -1 2 40 50 >>output.txt
diff expected_output.txt output.txt > difference.txt
#If there is any difference all the test cases didn't pass
if [ $? -eq 0 ]; 
then
    echo "All Test cases passed"
else
	echo "All Test cases not passed"
fi



