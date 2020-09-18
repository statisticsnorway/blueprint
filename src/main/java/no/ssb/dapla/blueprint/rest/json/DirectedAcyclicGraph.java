package no.ssb.dapla.blueprint.rest.json;


import java.util.*;

public class DirectedAcyclicGraph {

    private final List<NotebookDetail> nodes;
    private final Set<Edge> egdes = new LinkedHashSet<>();

    public DirectedAcyclicGraph(List<NotebookDetail> nodes) {
        this.nodes = new ArrayList<>(Objects.requireNonNull(nodes));
        // Compute the egdes.

        Map<String, NotebookDetail> byOutput = new HashMap<>();
        for (NotebookDetail notebookDetail : this.nodes) {
            for (String output : notebookDetail.getOutputs()) {
                byOutput.put(output, notebookDetail);
            }
        }

        for (NotebookDetail notebookDetail : this.nodes) {
            for (String input : notebookDetail.getInputs()) {
                if (byOutput.containsKey(input)) {
                    addEdge(byOutput.get(input), notebookDetail);
                }
            }
        }
    }

    public List<NotebookDetail> getNodes() {
        return nodes;
    }

    public Set<Edge> getEdges() {
        return egdes;
    }

    private void addEdge(NotebookDetail from, NotebookDetail to) {
        this.egdes.add(new Edge(
                from.getId(),
                to.getId()
        ));
    }

    public static class Edge {

        private final String from;
        private final String to;

        public Edge(String from, String to) {
            this.from = Objects.requireNonNull(from);
            this.to = Objects.requireNonNull(to);
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Edge edge = (Edge) o;
            return from.equals(edge.from) &&
                    to.equals(edge.to);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }
}
