# RIBS - REST-based Interface for BioStudies

<img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"> <img src="https://img.shields.io/badge/EMBL--EBI-BioStudies-green" alt="EMBL-EBI"> <img src="https://img.shields.io/badge/API-REST-orange" alt="REST API">

RIBS (REST-based Interface for BioStudies) is a RESTful API interface for interacting with the [BioStudies database](https://www.ebi.ac.uk/biostudies), EMBL-EBI's comprehensive repository for biological study data and metadata.

## About BioStudies

BioStudies is an EBI resource that holds descriptions of biological studies, links to supporting data in other databases, and archives data files that do not fit in existing public structured archives. The database organizes data from biological studies, typically associated with publications, offering a simple way to describe study structure.

## Features

- **RESTful API Access**: Programmatic access to BioStudies database functionality
- **Study Management**: Create, update, and retrieve biological study metadata
- **File Operations**: Handle study-associated files and datasets
- **Search Capabilities**: Query and search through study submissions
- **Authentication**: Secure access control for data submission and modification

## API Endpoints

The RIBS interface provides access to key BioStudies operations including:

- **Security & Authentication**: User authentication and session management
- **File Management**: Upload, download, and manage study files
- **Submissions**: Create and modify study submissions
- **Search**: Query and retrieve study information

For detailed API documentation, visit the [BioStudies API Documentation](https://biostudies.gitbook.io/biostudies-api).

## Getting Started

### Prerequisites

- Access to BioStudies database
- Valid authentication credentials
- HTTP client for REST API calls

## Installation

```bash
git clone https://github.com/EBIBioStudies/ribs.git
cd ribs
```

## Contributing

We welcome contributions to improve RIBS! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Make your changes
4. Add tests if applicable
5. Commit your changes (`git commit -am 'Add new feature'`)
6. Push to the branch (`git push origin feature/new-feature`)
7. Create a Pull Request

## Support

For issues and questions:

- **Technical Issues**: Open an issue on this repository
- **BioStudies Help**: Visit the [BioStudies help pages](https://www.ebi.ac.uk/biostudies/help)
- **API Documentation**: [BioStudies API Docs](https://biostudies.gitbook.io/biostudies-api)
- **Contact**: [EMBL-EBI Support](https://www.ebi.ac.uk/about/contact)

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Citation

If you use BioStudies in your research, please cite:

> Sarkans U, et al. The BioStudies database—one stop shop for all data supporting a life sciences study. Nucleic Acids Res. 2018;46(D1):D1266-D1270. doi:10.1093/nar/gkx965

## About EMBL-EBI

RIBS is developed and maintained by the European Molecular Biology Laboratory - European Bioinformatics Institute (EMBL-EBI), which provides freely available data from life science experiments, performs basic research in computational biology and offers an extensive user training programme.

---

**Maintained by**: [EBI BioStudies Team](https://github.com/EBIBioStudies)  
**Website**: [www.ebi.ac.uk/biostudies](https://www.ebi.ac.uk/biostudies)