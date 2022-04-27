package org.alfresco.rest.api.ext.search;

import org.alfresco.rest.api.model.Node;
import org.alfresco.rest.framework.resource.EntityResource;
import org.alfresco.rest.framework.resource.actions.interfaces.EntityResourceAction;
import org.alfresco.rest.framework.resource.parameters.Parameters;
import java.util.List;

@EntityResource(name="mltsearch", title = "MoreLikeThis")
public class MltSearchApiWebscript implements EntityResourceAction.Create<Node>
{

    @Override
    public List<Node> create(List<Node> list, Parameters parameters)
    {
        return List.of(new Node());
    }

}
