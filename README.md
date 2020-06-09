To analyze the user graph, please runs this:
sh run_analysis.sh

Note:
1.The output for the run_analysis.sh(it runs "src/main/ClusterAnalyzer.java") is stored in the file "analysis_results.txt", it contains the address that has paid money to FBI.that we thought to be or the silk road owners or users.
2.
"NumReceipts.txt" contains 2 columns, the first column is the number of receipts, and the second one is the user id.
3.
"MoneyReceived.txt" contains 2 columns, the first column is the total amount of money received, and the second one is the user id.
4.
"userGraph.txt" contains 3 columns, the first column is the input user id, the second one is the output user id, and the third one is the value of transferred bitcoin (in Satoshi) for each output.