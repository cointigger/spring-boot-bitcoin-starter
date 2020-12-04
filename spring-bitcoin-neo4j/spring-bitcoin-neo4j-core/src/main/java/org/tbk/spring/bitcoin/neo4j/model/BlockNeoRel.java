package org.tbk.spring.bitcoin.neo4j.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;


@Data
@RelationshipProperties
public class BlockNeoRel {

    @TargetNode
    private BlockNeoEntity block;
}
