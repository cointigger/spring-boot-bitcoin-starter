package org.tbk.bitcoin.txstats.example.model;

import com.google.common.collect.Lists;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.DateString;

import java.time.Instant;
import java.util.List;

@Data
@Node("tx_score")
public class TxScoreNeoEntity {
    @Id
    @GeneratedValue
    private Long id;

    // @Required
    @DateString
    private Instant createdAt;

    private long score;

    private boolean finalized;

    // @Required
    private String type;

    private List<String> labels = Lists.newArrayList();

    // @Required
    @Relationship(type = "SCORES", direction = Relationship.Direction.OUTGOING)
    private ScoresNeoRel tx;
}

