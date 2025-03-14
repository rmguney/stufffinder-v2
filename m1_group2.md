# Customer Milestone 1 Report (Group 2)

## **1. Project Summary**

### **Current Status**:
1. Requirements have been gathered and documented in the wiki.
2. User mockup scenarios and wireframes have been created.
3. Both Frontend and Backend from last semester have been merged and integrated.
4. Project is built, dockerized and deployed for both web and mobile platforms.
5. All features for milestone 1 have been implemented including user registration, post creation, commenting, voting, and search functionality.
6. Mobile compatibility has been significantly improved with responsive design.
7. Image storage has been migrated to Google Cloud for better reliability.

### **Planned Changes**:
1. Complete wireframes for enhanced UI/UX design (#44)
2. Implement advanced search functionality with range selection and optional attribute dropdowns
3. Add comment types (Guess, Discussion, Claim) as specified in customer feedback
4. Implement sorting options for search results (Solved/Unsolved, Most Trending, Most Recent)
5. Enhance user profile page to display bio and earned badges
6. Improve optional attributes UI/UX in post creation page
7. Implement predefined selections for attributes such as color
8. Add unit variety for attributes (cm/m, g/kg, $/€)
9. Address remaining review feedback for gathered requirements (#31)

## **2. Customer Feedback and Reflections**

### **Key Feedback**:
1. Optional attributes need better UI/UX design in post creation page. Optional attributes should be added if user wants to.
2. Some of the optional attributes should have pre-defined selections such as color.
3. Advanced search has to be included. The advanced search should provide optional range selection and optional attributes should be added from dropdown menu.
4. Main page post cards could display more information such as up/down vote count, comment count and date.
5. Comment types need to be added. These can be Guess, Discussion and Claim.
6. Users should be able to edit their posts.
7. Sorting options should be included for search feature. They could be Solved/Unsolved, Most Trending and Most Recent.
8. Unit attributes should have variety such as cm or m, grams or kg, $ or €.
9. Main page gray imaging needs to be removed.
10. Profile page needs to show user's bio as well as their earned badges.

### **Impact on the Project**:
1. The feedback on optional attributes will require UI/UX redesign of the post creation page, necessitating both frontend and backend changes.
2. Adding advanced search functionality will enhance usability but requires significant development in both frontend and backend components.
3. Implementing comment types requires database schema updates and UI changes to support different comment behaviors.
4. Adding sorting options will improve the search experience but requires backend query modifications and frontend UI updates.
5. Unit attributes with various options will require additional validation logic and UI controls.
6. Displaying more information on post cards will improve user experience but requires careful UI design to maintain clarity and performance.
7. Editing functionality for posts needs to be implemented to allow users to modify their content after submission.
8. These changes will be prioritized in the next sprint planning to meet customer expectations.

## **3. List and Status of Deliverables**
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

## **4. Reflection on Deliverables & Adjustments to Project Plan**

Evaluate whether deliverables align with the project timeline.

### **Challenges Faced**:

1. Performance issues with writable stores causing delay on loading times (#80) - This affected the frontend performance but was resolved through refactoring.
2. Integration difficulties between frontend and backend components from previous semester (#14, #26, #29, #30) - Required significant coordination between team members.
3. Authentication and security configuration issues (#86, #93) - Improved by moving from local storage to cookies.
4. Mobile display compatibility required significant refactoring of shared components (#27, #28, #33, #39, #49, #54, #59) - More effort than initially estimated.
5. Threadstore implementation problems that required multiple iterations (#73, #80, #84).
6. Image storage challenges that led to migration to Google Cloud (#108) - Enhanced reliability but required additional work.
7. Visual inconsistencies across platforms required CSS fixes (#64, #67, #71).
8. Search functionality limitations that needed enhancement (#77, #98, #101).
9. Some team members had to overwork due to some members' inability to work on the project.

### **Adjustments to Project Plan**:

1. Added additional time for mobile compatibility work which required more effort than initially anticipated.
2. Prioritized image storage migration to Google Cloud for more reliable media handling.
3. Allocated resources to improve search functionality with semantic tags based on customer feedback.
4. Revised authentication approach to use cookies instead of local storage for better security.
5. Added tasks to address UI/UX improvements for optional attributes in post creation.
6. Reorganized development workflow to better handle the integration of legacy code components.
7. Implemented more robust deployment processes including dockerization for consistent environments.

## **5. Tools & Processes for Team Management**

- **Collaboration Tools**: GitHub for code management and documentation, WhatsApp for quick communication, Google Meet for virtual meetings
- **Project Tracking**: GitHub Projects for task management, Issues for tracking feature implementation and bug fixes
- **Version Control & Code Review**: GitHub Pull Requests with required reviews before merging
- **CI/CD**: Configured GitHub protection rules and workflows (#17) to ensure code quality
- **Deployment**: Docker containers for consistent development and production environments (#58, #110)
- **Documentation**: Wiki pages for comprehensive documentation and GitHub repository for code documentation

## **6. Requirements Addressed in This Milestone**

### **Implemented Features**:

#### User Management
1. Users can register with a unique username and password (1.1.1, #21).
2. Login and logout functionalities are implemented (1.1.2, #25).
3. A user profile is created upon registration with username and email (1.2.1).
4. Users can post content anonymously (1.3.1).

#### Content Creation and Management
5. Users can create posts to identify or learn about mystery objects (2.1.1, #19).
6. Posts include a title, description, and image as mandatory (2.1.2).
7. Users can provide optional attributes such as material, dimensions, time period, weight, texture, color, smell, shape, location, hardness, functionality, price, origin of acquisition, pattern, handmade, one of a kind, and markings (2.1.3).
8. The system provides structured form and input fields for attributes (2.1.4).
9. Users can tag posts with semantic tags from Wikidata for better insight (2.1.5, #53).
10. Users can upload images to accompany their posts (2.2.1).
11. The system supports common media formats (2.2.2).
12. Users can mark posts as "Solved" or "Unsolved" (2.3.1).
13. The system displays the status of each post prominently (2.3.2).
14. Solved posts show the best answer on top  and credit the solver (2.3.3, 2.3.4).

#### Interaction and Feedback
15. Users can comment on posts and engage in threaded discussions (3.1.2, #26).
16. Post authors can mark specific comments as solutions (3.2.1).
17. Solved comments are highlighted in the comment section (3.2.3).
18. Users can upvote or downvote posts (#29) and comments (#30) based on usefulness (3.3.1).
19. The system displays vote count for each post and comment (3.3.2).

#### Search and Navigation
20. Users can search posts by keywords, tags, and attributes (4.1.1, #50, #77, #98).
21. Users can tag posts with Wikidata tags (4.2.1, 4.2.2).

#### Notifications and Alerts
22. Users receive notifications for new comments on their posts (5.1.1, #32).
23. Users receive notifications when their comment is marked as a solution (5.1.2, #32).
24. Users receive notifications when their posts or comments are upvoted or downvoted (5.1.3, #32).

#### Platform Support
25. A fully working Android application has been released.

## **7. Individual Contributions**

Graphs to provide an overview of the trackable effort, aside from timeline logs, can be found on:

- [Contribution Insight](https://github.com/rmguney/swe574-2025s-g2/graphs/contributors)  
- [Project Backlog](https://github.com/users/rmguney/projects/1)  

### Member: Raşit Melih Güney (rmguney)

Responsibilities: Frontend development, mobile compatibility, integration, DevOps

Main contributions: Led the frontend and mobile development efforts, handled integration challenges, improved performance and UI/UX, created issues and related documentations.

Code-related significant issues:
- Integrated post display API (#37, #38)
- Modified shared components for better mobile display (#27, #28, #33, #39, #49, #54, #59)
- Fixed datetime display on thumbnail posts (#71, #72)
- Improved threadstore performance (#80, #82)
- Prepared Android configurations (#105)

Non-code-related significant issues:
- Created milestones (#13)
- Set up communication channels (#4)
- Created wiki (#3)

Pull requests: Created and merged multiple PRs including #38, #51, #55, #69, #70, #72, #75, #79, #82

### Member: Mustafa Bektaş (mustafa-bektas)

Responsibilities: Backend & frontend development, API integration, search functionality, dockerization and deployment, documentation, issue and PR management

Main contributions: Developed and enhanced backend & frontend services, implemented search features, handled dockerization and deployment.

Code-related significant issues:
- Integrated API with frontend (#14, #26, #29, #30)
- Implemented profile page (#42, #43)
- Enhanced search functionality (#50, #77, #98, #101)
- Improved authentication security (#86, #87)
- Migrated stored images to Google Cloud (#108, #109)

Non-code-related significant issues:
- Created API endpoint documentation (#18)
- Dockerized and deployed the project (#58, #60)

Pull requests: Created and merged multiple PRs including #43, #48, #52, #56, #57, #60, #85, #87, #91, #94, #97, #99, #102, #109

### Member: Mert Ünlü (MertUnlu-SWE)

Responsibilities: Documentation, requirements gathering.

Main contributions: Managed documentation, gathered requirements and handled wiki pages.

Code-related significant issues:
- Fixed semantic tag display in profiles (#76)

Non-code-related significant issues:
- Created requirement elicitation questions (#2)
- Gathering Requirements and creation of requirements wiki page (#8)
- Documented weekly meeting notes (#7)
- Created issue structure guidelines (#15)
- Created user scenarios (#11)
- Created project plan wiki page (#111)
- Keep track of issues and issue labels (#12)