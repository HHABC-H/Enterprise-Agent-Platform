package com.agent.document;

import java.util.List;

public interface DocumentSplitter {

    List<Chunk> split(ParsedDocument document);
}
