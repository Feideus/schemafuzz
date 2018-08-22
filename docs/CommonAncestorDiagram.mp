\documentclass[border=10pt]{standalone}
\usepackage{tikz}
\begin{document}
\begin{tikzpicture}[sibling distance=10em,
  every node/.style = {shape=rectangle, rounded corners,
    draw, align=center,
    top color=white, bottom color=blue!20}]]
  \node {Root}
    child { node {Mutation1} 
	  child { node {Mutation2}
        child { node {Mutation3} }
        child { node {Mutation4} }
        child { node {Mutation5} } }
      child { node {Mutation6} } };
    child { node {Mutation7}
      child { node {Mutation8}
        child { node {Mutation9} }
        child { node {Mutation10} }
        child { node {Mutation11} } }
      child { node {Mutation12} } };
\end{tikzpicture}
\end{document}
