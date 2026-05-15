package com.flowdeck.backend.file.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class FlowFileTest {

  @Test
  void createInitializesProjectFileNode() {
    FlowFile flowFile = FlowFile.create(1L, null, "src", FileType.FOLDER);

    assertEquals(1L, flowFile.getProjectId());
    assertNull(flowFile.getParentId());
    assertEquals("src", flowFile.getName());
    assertEquals(FileType.FOLDER, flowFile.getType());
    assertEquals(0, flowFile.getCurrentVersion());
  }

  @Test
  void renameChangesName() {
    FlowFile flowFile = FlowFile.create(1L, null, "Flow.java", FileType.FILE);

    flowFile.rename("Flow-v2.java");

    assertEquals("Flow-v2.java", flowFile.getName());
  }

  @Test
  void issueNextVersionNumberIncrementsSequentially() {
    FlowFile flowFile = FlowFile.create(1L, null, "Flow.java", FileType.FILE);

    int firstVersion = flowFile.issueNextVersionNumber();
    int secondVersion = flowFile.issueNextVersionNumber();

    assertEquals(1, firstVersion);
    assertEquals(2, secondVersion);
  }
}
