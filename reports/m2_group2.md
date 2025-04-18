# Customer Milestone 2 Report (Group 2)

## **1. Project Summary**

### **Major changes since Milestone 1:**

1. Added multiple parts functionality for the objects.
2. Added better property handling for the objects.
3. Improved mobile responsiveness and added mobile native gestures.
4. Fixed storage related issues.
5. Added multiple multimedia support for posts and comments.
6. Finalized requirements and feature-related decisions.
7. Added editing functionalities for the post/comment authors.
8. Overhauled visuals with more clarity and consistency.
9. Fixed leftover bugs from the Customer Milestone 1.
10. Implemented semantic tag searching.

### **Done, but not included in the milestone:**

1. Add comment types (Guess, Discussion, Claim) as specified in customer feedback
2. Implement sorting options and pagination for both homepage and search results (Solved/Unsolved, Most Trending, Most Recent)
3. Implement resolution feature addressed in the requirements
4. Better handling of tags

### **Planned changes:**

1. Enhance user profile page to display bio, earned badges and reputation
2. Address remaining review feedback for specified requirements
3. Advanced search integration into semantic search
4. Reporting and removal of inappropriate content

## **2. Customer feedback and reflections**

### **Key feedback:**

1. Object parts need to be clearer. Users should not be forced to add a “base” part if it’s not necessary. The relationship between main objects and their parts should be better explained.
2. Tagging should be more consistent. Using general or unclear tags like “mystery” is not helpful. Tags should be relevant and, if possible, connected to Wikidata.
3. Having two different types of tags has to be avoided. If users can create their own tags (user-defined), this makes the system harder to manage. It’s better if all tags have a clear meaning and are semantically linked (for example, using Wikidata).
4. Each object should include a story. This part of the platform is very important and shouldn’t be left out in the final version.
5. All features have to be made sure to be listed in the requirements. If a feature works in the demo, it should also be written as a system requirement in the report, with input/output behavior explained.
6. Improve advanced search filters. Filters like material, size, and weight should be added. Use sliders for numeric values like weight (less than 1 kg).
Semantic search should show grouped results. For example, if someone searches “metal,” the results can be grouped into copper, aluminum, etc. to make it easier to understand.
7. Resolution system still needs to be added. This is a required feature and must be finished before the final submission.
8. Comment types still needs to be addded. This is a required feature and must be finished before the final submission.

### **Impact on the project:**

1. Adding advanced search functionality will enhance usability but requires significant development in both frontend and backend components.
2. Implementing comment types requires database schema updates and UI changes to support different comment behaviors.
3. Adding sorting and filtering options will improve the search experience but requires backend query modifications and frontend UI updates.
4. Adding resolution feature will provide more information on a mystery objects discovery but requires careful development in both backend and frontend.
5. These changes will be prioritized in the final sprint planning to meet customer expectations.

## **3. List and status of deliverables**

Table of each deliverable with links and its status:

| **Deliverable** | **Status** | **Link** |
|---------------|------------|--------------------------|
| Software Requirements Specification | Completed | [https://github.com/rmguney/swe574-2025s-g2/wiki/03.-Requirements](https://github.com/rmguney/swe574-2025s-g2/wiki/03.-Requirements) |
| UML Diagrams | Completed | [https://github.com/rmguney/swe574-2025s-g2/wiki/08.-UML-Diagrams](https://github.com/rmguney/swe574-2025s-g2/wiki/08.-UML-Diagrams) |
| Scenarios and Mockups | Completed | [https://github.com/rmguney/swe574-2025s-g2/wiki/02.-User-Scenarios](https://github.com/rmguney/swe574-2025s-g2/wiki/02.-User-Scenarios), [https://github.com/rmguney/swe574-2025s-g2/wiki/07.-Wireframes](https://github.com/rmguney/swe574-2025s-g2/wiki/07.-Wireframes) |
| Project Plan | Completed | [https://github.com/rmguney/swe574-2025s-g2/wiki/11.-Project-Plan](https://github.com/rmguney/swe574-2025s-g2/wiki/11.-Project-Plan) |
| Communication Plan | Completed | [https://github.com/rmguney/swe574-2025s-g2/wiki/09.-Communication-Plan](https://github.com/rmguney/swe574-2025s-g2/wiki/09.-Communication-Plan) |
| Weekly Notes | In Progress | [https://github.com/rmguney/swe574-2025s-g2/wiki/01.-Meeting-Notes-and-Progress-Tracking](https://github.com/rmguney/swe574-2025s-g2/wiki/01.-Meeting-Notes-and-Progress-Tracking) |
| Pre-release Version | Completed | [https://github.com/rmguney/swe574-2025s-g2/releases/tag/0.1.0](https://github.com/rmguney/swe574-2025s-g2/releases/tag/0.1.0) |
| Installation Guide | Completed | [https://github.com/rmguney/swe574-2025s-g2/wiki/10.-Installation-Guides](https://github.com/rmguney/swe574-2025s-g2/wiki/10.-Installation-Guides) |

## **4. Challenges faced:**

1. Visual inconsistencies across platforms required fixes.  
2. Uploading multi-media content caused unexpected duplicate content in the cloud.
3. Authentication errors were faced when trying to create posts after database wipe.
4. Visual errors were faced for adding a color to a sub-part. It showed the correct name but the color visually looked different. (#160, #162)
5. Visual error was encountered after saving a sub-part, was only showing material, color, shape and texture attributes.
6. One team member was assigned an issue for post resolution but did it follow best practices for development and did not complete it for the given deadline. This caused other members to re-plan for the milestone.

### **Adjustments to project plan to adress challenges:**

1. Added additional time for mobile compatibility work which required more effort than initially anticipated.
2. Implemented to improve UI/UX for optional attributes in post creation.
3. Prioritized predefined attribute such as color picker for attribute addition in post.
4. Prioritized creating and editting sub-part of mystery objects.
5. Prioritized support for multi-media content.
6. Allocated resources to improve search functionality with semantic tags based on customer feedback.

## **5. Tools & processes for team management**

- **Collaboration Tools**: GitHub for code management and documentation, WhatsApp for quick communication, Google Meet for virtual meetings
- **Project Tracking**: GitHub Projects for task management, Issues for tracking feature implementation and bug fixes
- **Version Control & Code Review**: GitHub Pull Requests with required reviews before merging
- **CI/CD**: Configured GitHub protection rules and workflows (#17) to ensure code quality
- **Deployment**: Docker containers for consistent development and production environments (#58, #110)
- **Documentation**: Wiki pages for comprehensive documentation and GitHub repository for code documentation

## **6. Requirements adressed in this milestone**

### **Implemented features:**

#### User management

1. The registration form has been enhanced to include fields like email and password confirmation, addressing user input validation needs. (1.1.1, #121).
2. Users can now upload multi-media content such as images and videos when creating a post. This ensures posts have the required visual context for identification. (2.1.1, 2.2.1) (#116, #125).
3. The system supports common media formats (2.2.2).
4. The post creation form has been expanded to allow entry of detailed optional attributes like material, dimensions, and sub-parts. This supports better classification and identification. (2.1.2) (#123, #151, #132).
5. The system now includes predefined selection tools for some attributes, such as color picker, material to improve data consistency and user experience. (2.1.3) (#118, #152).
6. The system provides structured form and input fields for attributes (2.1.4).
7. Users can edit their posts, able to change mandatory fields as well as optional attributes (2.1.5).
8. The comment system has been improved to support the upload of multi-media, such as images. This enhancement enables users to provide visual evidence or examples when commenting. (3.1.4, #120).
9. Users can edit their comments (3.1.5).
10. Solved comments are highlighted in the comment section (3.2.3).
11. The system displays vote count for each post and comment (3.3.2).
12. Mobile navigation has been improved with gesture support and a carousel display for a better experience on smaller screens. (4.1.1, #129, #148).
13. Users can tag posts with Wikidata tags (4.2.1, 4.2.2).
14. A fix has been implemented for the notification system, allowing users to mark all alerts as read, improving usability and reducing clutter. (5.1.1, #136)
15. Several updates were made to improve visual consistency across the platform, particularly for thread pages, color displays, and the overall UI design. (#144, #150, #153, #130)
16. The project has been successfully prepared for Android deployment, including mobile-specific visual and performance optimizations. (#146)

## **7. Individual contributions**

Graphs to provide an overview of the trackable effort, aside from timeline logs, can be found on:

- [Contribution Insight](https://github.com/rmguney/swe574-2025s-g2/graphs/contributors)  
- [Project Backlog](https://github.com/users/rmguney/projects/1)  

### Member: Melih Güney (rmguney)

Responsibilities: Frontend andmobile development, repo management, documentation, issue creation, code reviews, bugfixes

Significant issues:

- prepare android configs for faster iteration #105
- create installation guides for both docker and local #110
- add multimedia support to both post creation page and post display #113
- post creation - color should be chosen from color wheel #118
- swipe gesture support to go back on mobile devices #129
- materials selection suggestions #130
- general visual consistency checks #131, visual bugs after new features #136

### Member: Mustafa Bektaş (mustafa-bektas)

Responsibilities: Backend and frontend development, DevOps, documentation, issue creation, code reviews, bugfixes

Significant issues:

- revamp attributes page #92
- semantic Search #101
- comments should support multimedia #120
- mystery objects should consist of multiple parts #122
- endpoints for multiple media uploads #125
- edit comment functionality for the author #128
- deploy the latest version of the app on the cloud #169

### Member: Mert Ünlü (MertUnlu-SWE)

Responsibilities: Documentation, requirements specifying, tidying up issues, frontend development

Significant issues:

- documented weekly meeting notes #7
- gather the requirements and create a requirements page in the wiki #8
- better post display for the main page (#138)
- wipe database and re-populate with new data (#165)
- sorting, filtering and pagination for search results #123 (not yet merged due to a bug)