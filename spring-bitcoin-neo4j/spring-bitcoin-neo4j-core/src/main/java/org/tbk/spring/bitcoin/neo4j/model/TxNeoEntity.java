package org.tbk.spring.bitcoin.neo4j.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(of = "txid")
@Node("tx")
public class TxNeoEntity {
    @Id
    private String txid;

    /**
     * Transaction data format version (note, this is signed)
     */
    private long version;

    /**
     * Number of Transaction inputs (never zero)
     */
    private long txincount;

    /**
     * Number of Transaction outputs
     */
    private long txoutcount;

    private long locktime;

    @CompositeProperty(prefix = "meta")
    private Map<String, Object> meta = new HashMap<>();

    @Relationship(type = "INCLUDED_IN", direction = Relationship.Direction.OUTGOING)
    private IncludedInNeoRel block;

    @Relationship(type = "IN", direction = Relationship.Direction.INCOMING)
    private List<OutNeoRel> inputs;

    @Relationship(type = "OUT", direction = Relationship.Direction.OUTGOING)
    private List<OutNeoRel> outputs;
}

