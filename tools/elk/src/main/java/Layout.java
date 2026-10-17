import java.nio.charset.StandardCharsets;

import org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.data.LayoutMetaDataService;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.json.ElkGraphJson;

/** ELK JSON in on stdin, the same graph with coordinates out on stdout. */
public final class Layout {
    public static void main(String[] args) throws Exception {
        LayoutMetaDataService.getInstance()
            .registerLayoutMetaDataProviders(new LayeredMetaDataProvider());
        String in = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
        ElkNode graph = ElkGraphJson.forGraph(in).toElk();
        new RecursiveGraphLayoutEngine().layout(graph, new BasicProgressMonitor());
        System.out.println(ElkGraphJson.forGraph(graph)
            .omitLayout(false).omitZeroDimension(true).prettyPrint(false).toJson());
    }
}
