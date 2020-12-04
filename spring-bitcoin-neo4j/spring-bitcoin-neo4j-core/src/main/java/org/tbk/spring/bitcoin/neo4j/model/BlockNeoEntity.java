package org.tbk.spring.bitcoin.neo4j.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.DateString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(of = "hash")
@Node("block")
public class BlockNeoEntity {
    @Id
    private String hash;

    /**
     * Block version information (note, this is signed)
     */
    private long version;

    /**
     * The reference to a Merkle tree collection which is a hash of all transactions related to this block
     */
    // @NonNull
    private String merkleroot;

    /**
     * A Unix timestamp recording when this block was created (Currently limited to dates before the year 2106!)
     */
    // @NonNull
    @DateString
    private Instant time;

    /**
     * The calculated difficulty target being used for this block
     */
    private long difficulty;

    /**
     * The nonce used to generate this block… to allow variations of the header and compute different hashes
     */
    private long nonce;

    @CompositeProperty(prefix = "meta")
    private Map<String, Object> meta = new HashMap<>();

    /**
     * The hash value of the previous block this particular block references
     */
    @Relationship(type = "PREV_BLOCK", direction = Relationship.Direction.OUTGOING)
    private BlockNeoRel prevblock;

    @Relationship(type = "BASED_ON", direction = Relationship.Direction.INCOMING)
    private BlockNeoRel nextblock;

    @Relationship(type = "INCLUDED_IN", direction = Relationship.Direction.INCOMING)
    private List<TxNeoEntity> transactions = new ArrayList<>();
}

