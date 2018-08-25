\section{Usage}
		\subsection{prerequisites}
			SchemaFuzz requires the presence of a list of libraries to work properly which are :
			\begin{itemize}
			\item org.apache.commons.math3 >= 3.6
			available at \\*
			\url{https://commons.apache.org/proper/commons-math/download_math.cgi}			
			\end{itemize}
The library has to be installed in the maven repository to be available. The instructions detailed at the following address explain how to do that. futher information can be found on the official maven website.\\*

			\url{https://www.mkyong.com/maven/how-to-include-library-manully-into-maven-local-repository/}
			
		\subsection{setting up the code}
			Once all the depencies have been installed successfully, clone the source available on the official git taler repository \\*
			\url{https://git.taler.net/schemafuzz.git}
			\begin{verbatim}
			 git clone https://git.taler.net/schemafuzz.git
			\end{verbatim}
			
the folder containing the code shoud hold the rights for reading writing and executing (rwx) for the user that plans to run the tool.
if this is not the case, you can give these rights like so
			\begin{verbatim}
			sudo chmod -R 700 schemafuzz
			\end{verbatim}
		\subsection{Building}
SchemaFuzz is using maven for building and library management purposes.
Therefore, using the maven command line building script is way to go.
Standard way of building :\\*
			\begin{verbatim}
			./mvnw package
			\end{verbatim}
				
This maven building method also offers alternative instructions for 	more precise/refined way of building as well as compilation and test 
launching options (those should only be intresting for the contributors).

Launching the test suit :\\*
			\begin{verbatim}
			./mvnw test
			\end{verbatim}
Compiling the code :\\*		
			\begin{verbatim}
			./mvnw compile
			\end{verbatim}
		
Other usefull commands: \\*		
		
			\begin{verbatim}
			./mvnw clean
			\end{verbatim}
			\begin{verbatim}
			./mvnw validate
			\end{verbatim}
			\begin{verbatim}
			./mvnw deploy
			\end{verbatim}
		
		\subsection{Setting up the database}	
	
Launch the "dbConfigure" script.
			\begin{verbatim}
				./dbConfigure
			\end{verbatim}		 
	
