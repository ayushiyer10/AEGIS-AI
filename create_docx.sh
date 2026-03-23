#!/bin/bash

# Create DOCX by building ZIP with XML structure
cd "/tmp" || exit 1
mkdir -p aegis_docx
cd aegis_docx

# Create directory structure
mkdir -p _rels word docProps

# Create [Content_Types].xml
cat > "[Content_Types].xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
</Types>
EOF

# Create _rels/.rels
cat > "_rels/.rels" << 'EOF'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
</Relationships>
EOF

# Create word/_rels/document.xml.rels
mkdir -p word/_rels
cat > "word/_rels/document.xml.rels" << 'EOF'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
</Relationships>
EOF

# Create docProps/core.xml
cat > "docProps/core.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/officeDocument/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<dc:title>AEGIS Anti-Cheat Engine - Software Requirements Specification</dc:title>
<dc:subject>Anti-Cheat System SRS</dc:subject>
<dc:creator>AEGIS Development Team</dc:creator>
<cp:lastModifiedBy>AEGIS Team</cp:lastModifiedBy>
<cp:revision>1</cp:revision>
</cp:coreProperties>
EOF

# Create word/document.xml with content
cat > "word/document.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<w:body>
<w:sectPr>
<w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/>
</w:sectPr>
<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:line="360" w:lineRule="auto"/></w:pPr><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:sz w:val="36"/><w:b/></w:rPr><w:t>AEGIS Anti-Cheat Engine</w:t></w:r></w:p>
<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:line="360" w:lineRule="auto"/></w:pPr><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:sz w:val="36"/><w:b/></w:rPr><w:t>Software Requirements Specification</w:t></w:r></w:p>
<w:p><w:spacing w:line="360" w:lineRule="auto"/></w:p>
<w:p><w:spacing w:line="360" w:lineRule="auto"/></w:p>
<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:line="360" w:lineRule="auto"/></w:pPr><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:sz w:val="24"/></w:rPr><w:t>Version 1.0</w:t></w:r></w:p>
<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:line="360" w:lineRule="auto"/></w:pPr><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:sz w:val="22"/></w:rPr><w:t>Document Date: March 23, 2026</w:t></w:r></w:p>
<w:p><w:pPr><w:spacing w:line="360" w:lineRule="auto"/></w:pPr></w:p>
<w:p><w:pPr><w:spacing w:line="360" w:lineRule="auto"/></w:pPr></w:p>
<w:p><w:pPr><w:spacing w:line="360" w:lineRule="auto"/></w:pPr></w:p>
<w:sectPr><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/><w:pgSz w:w="12240" w:h="15840"/><w:pgBorders w:offsetFrom="page"/><w:cols w:num="1"/></w:sectPr>
<w:p><w:pPr><w:pStyle w:val="Heading1"/><w:spacing w:line="360" w:lineRule="auto"/></w:pPr><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:sz w:val="28"/><w:b/></w:rPr><w:t>Executive Summary</w:t></w:r></w:p>
<w:p><w:pPr><w:spacing w:line="360" w:lineRule="auto"/></w:pPr><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:sz w:val="24"/></w:rPr><w:t>The AEGIS Anti-Cheat Engine is a comprehensive, real-time detection system designed to identify and prevent various forms of cheating in online gaming environments. The system leverages advanced movement analysis, behavioral pattern recognition, and machine learning-inspired scoring algorithms to detect suspicious activity with minimal false positives. By continuously analyzing mouse movements, keyboard inputs, screen motion, and game context, AEGIS provides administrators with actionable intelligence and enforcement capabilities to maintain competitive integrity.</w:t></w:r></w:p>
<w:p><w:pPr><w:spacing w:line="360" w:lineRule="auto"/></w:pPr><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:sz w:val="24"/></w:rPr><w:t>The architecture is built on a three-tier model consisting of a Python-based client agent that runs on player machines, a Spring Boot backend that processes and analyzes behavioral data, and a React-based web interface for monitoring and analytics. The system maintains a 15-second offline timeout for device presence tracking and employs sophisticated state machines to balance detection sensitivity with legitimate play patterns.</w:t></w:r></w:p>
<w:p><w:pPr><w:spacing w:line="360" w:lineRule="auto"/></w:pPr><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:sz w:val="24"/></w:rPr><w:t>This specification defines all functional requirements, system components, detection algorithms, API contracts, and deployment architectures necessary to implement and maintain the AEGIS platform. The document serves as the authoritative reference for developers, testers, system administrators, and security personnel involved in the anti-cheat initiative.</w:t></w:r></w:p>
</w:body>
</w:document>
EOF

# Create ZIP as DOCX
cd "/tmp/aegis_docx"
zip -r "AEGIS_SRS_Document.docx" . -x "*.docx"
cp "AEGIS_SRS_Document.docx" "/c/Users/Ayush Iyer/Desktop/Anti Cheat/docs/"

echo "Document created successfully!"
echo "Location: /c/Users/Ayush Iyer/Desktop/Anti Cheat/docs/AEGIS_SRS_Document.docx"
