package org.tbk.bitcoin.txstats.example.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.tbk.spring.bitcoin.neo4j.model.TxNeoEntity;

@Data
@RelationshipProperties
public class ScoresNeoRel {

    @TargetNode
    private TxNeoEntity target;
}
