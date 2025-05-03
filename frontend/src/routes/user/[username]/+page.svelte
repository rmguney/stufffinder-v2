<script>
  import * as Card from '$lib/components/ui/card/index.js';
  import { Button } from '$lib/components/ui/button';
  import Post from '$lib/components/post.svelte';
  import Comment from '$lib/components/comment.svelte';
  import { threadStore } from '../../../threadStore';
  import { page } from '$app/stores';
  import { onMount } from 'svelte';
  import { PUBLIC_API_URL } from "$env/static/public";
  import { fetchTagLabels, tagMap } from "$lib/utils/fetchTags.js";
  import { Tabs, TabsContent, TabsList, TabsTrigger } from "$lib/components/ui/tabs";
  import { activeUser } from '../../../userStore';
  import UserCountrySelector from '$lib/components/userCountrySelector.svelte';
  
  // State variables
  let userId;
  let userName;
  let threads = [];
  let comments = [];
  let loadingThreads = true;
  let loadingComments = true;
  let tagsLoaded = false;
  let userBio = "";
  let userImage = null;
  let userCountries = []; // Selected countries (up to 3)
  let userBadges = []; // Store fetched badges
  let isCurrentUserProfile = false;
  let isFollowing = false;
  let followLoading = false;
  let followerCount = 0;
  let followingCount = 0;
  let userNotFound = false;
  let savingProfile = false; // Track profile saving state
  let saveError = null; // Track profile save errors
  let uploadingImage = false;
  let uploadError = null;
  let allCountriesData = []; // Add state for all countries
  let initialUserBio = ""; // Store initial bio value
  let profileLoaded = false; // Track if initial profile load is complete
  
  // Pagination state
  let postsPage = 1;
  let commentsPage = 1;
  const itemsPerPage = 10;
  
  // Computed properties for pagination
  $: paginatedThreads = threads.slice((postsPage - 1) * itemsPerPage, postsPage * itemsPerPage);
  $: paginatedComments = comments.slice((commentsPage - 1) * itemsPerPage, commentsPage * itemsPerPage);
  $: totalPostPages = Math.ceil(threads.length / itemsPerPage);
  $: totalCommentPages = Math.ceil(comments.length / itemsPerPage);
  
  // Get username from URL params
  $: userName = $page.params.username;
  $: isCurrentUserProfile = $activeUser === userName;
  $: bioChanged = userBio !== initialUserBio; // Reactive check if bio has changed
  
  // Format date in the same style as the Angular implementation
  function formatDate(date) {
    if (!date) return 'Unknown date';
    try {
      return new Date(date).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    } catch (error) {
      console.error("Error formatting date:", error);
      return 'Unknown date';
    }
  }
  
  // Process thread data to ensure all expected fields exist
  function processThreadData(thread) {
    return {
      ...thread,
      title: thread.title || "Untitled Post",
      description: thread.description || thread.content || "",
      createdAt: thread.createdAt || thread.created || thread.date || null,
      tags: Array.isArray(thread.tags) ? thread.tags : 
                  (thread.categories ? thread.categories : []),
            resolvedTags: thread.tags?.map(qcode => tagMap.get(qcode) || "Loading...") || [],
            // Add image URL processing from mystery object when available
            mediaFiles: processThreadMediaFiles(thread)
    };
  }

  // Completely rewritten media processing function that handles all edge cases
  function processThreadMediaFiles(thread) {
    console.log("Processing media for thread:", thread.id, thread.title);
    const mediaFiles = [];
    
    try {
      // Check directly for image URL in the thread
      if (thread.imageUrl) {
        console.log("Found direct imageUrl:", thread.imageUrl);
        mediaFiles.push({
          type: 'image',
          url: thread.imageUrl
        });
      }
      
      // Check for mediaFiles array with proper structure
      else if (thread.mediaFiles && Array.isArray(thread.mediaFiles) && thread.mediaFiles.length > 0) {
        console.log("Found mediaFiles array with length:", thread.mediaFiles.length);

        thread.mediaFiles.forEach((media, index) => {
          if (media.id) {
            const url = `${PUBLIC_API_URL}/api/mysteryObjects/media/${media.id}`;
            console.log(`Adding media ${index} with ID ${media.id}, URL: ${url}`);

            mediaFiles.push({
              id: media.id,
              type: media.contentType?.startsWith('image/') ? 'image' : 'unknown',
              url: url,
              contentType: media.contentType
            });
          }
        });
      }
      
      // Check for mystery object with various image properties
      else if (thread.mysteryObject) {
        console.log("Checking mysteryObject for images");
        const mo = thread.mysteryObject;
        
        // Try direct imageUrl
        if (mo.imageUrl) {
          console.log("Found mysteryObject.imageUrl:", mo.imageUrl);
          mediaFiles.push({
            type: 'image',
            url: mo.imageUrl
          });
        }
        // Try image ID
        else if (mo.imageId) {
          const url = `${PUBLIC_API_URL}/api/mysteryObjects/media/${mo.imageId}`;
          console.log("Found mysteryObject.imageId:", mo.imageId, "URL:", url);
          mediaFiles.push({
            type: 'image',
            url: url
          });
        }
        // Try media array in mystery object
        else if (mo.media && Array.isArray(mo.media) && mo.media.length > 0) {
          console.log("Found mysteryObject.media array with length:", mo.media.length);
          mo.media.forEach((media, index) => {
            if (media.id) {
              const url = `${PUBLIC_API_URL}/api/mysteryObjects/media/${media.id}`;
              console.log(`Adding mystery object media ${index} with ID ${media.id}, URL: ${url}`);

              mediaFiles.push({
                id: media.id,
                type: 'image', // Assuming all mystery object media are images
                url: url
              });
            }
          });
        }
      }
      
      // As a last resort, check for base64 encoded image
      else if (thread.mysteryObjectImage) {
        console.log("Found base64 mysteryObjectImage");
        mediaFiles.push({
          type: 'image',
          url: `data:image/png;base64,${thread.mysteryObjectImage}`
        });
      }

      console.log(`Total media files found for thread ${thread.id}: ${mediaFiles.length}`);
    } catch (error) {
      console.error("Error processing thread media:", error);
    }
    
    return mediaFiles;
  }

  // Fetches all q codes and updates tags inside the thread
  async function updateTags() {
    const allQcodes = threads.flatMap(thread => thread.tags).filter(qcode => qcode);

    if (allQcodes.length > 0) {
      await fetchTagLabels(allQcodes);
      tagsLoaded = true;

      // Update tags inside threads
      threads = threads.map(thread => ({
                ...thread,
                resolvedTags: thread.tags.map(qcode => tagMap.get(qcode) || qcode)
      }));
    }
  }
  
  // Enhanced function for image upload
  async function handleImageUpload(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    // Check file type
    if (!file.type.startsWith('image/')) {
      uploadError = "Please select an image file";
      return;
    }
    
    // Check file size (limit to 5MB)
    if (file.size > 5 * 1024 * 1024) {
      uploadError = "Image must be less than 5MB";
      return;
    }
    
    try {
      uploadingImage = true;
      uploadError = null;
      
      // Create FormData object
      const formData = new FormData();
      formData.append('file', file); // Use 'file' key as expected by backend

      // Upload to server using authenticated fetch (assuming global fetch handles auth)
      const response = await fetch(`${PUBLIC_API_URL}/api/users/me/profile-picture`, { 
        method: 'POST',
        body: formData
        // No need for credentials: 'include' if global fetch handles it
      });

      if (!response.ok) {
        throw new Error(`Upload failed with status: ${response.status}`);
      }
      
      const data = await response.json();

      // Update profile image with the URL returned from server
      if (data.profilePictureUrl) { // Use 'profilePictureUrl' key
        userImage = data.profilePictureUrl;
      } else {
        // Fallback if URL is missing but response is ok (shouldn't happen ideally)
        userImage = URL.createObjectURL(file);
      }
      
      // Success notification could be added here
      
    } catch (error) {
      console.error("Error uploading profile image:", error);
      uploadError = "Failed to upload image. Please try again.";
    } finally {
      uploadingImage = false;
    }
  }

  // Function to save Bio and Location
  async function saveProfile() {
    if (!isCurrentUserProfile) return;

    savingProfile = true;
    saveError = null;

    try {
      // Map the array of country objects to an array of country codes (cca2)
      const countryCodes = userCountries.map(country => country.cca2);

      const payload = {
        bio: userBio,
        location: countryCodes // Send the array of codes
      };

      // Use global fetch (assuming it handles auth)
      const response = await fetch(`${PUBLIC_API_URL}/api/users/me/profile`, { 
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Failed to save profile' }));
        throw new Error(errorData.message || `Save failed with status: ${response.status}`);
      }

      const updatedProfile = await response.json();

      // Optionally update local state if needed, though a page refresh might be simpler
      userBio = updatedProfile.bio;
      try {
        // Location comes back as a JSON string from the DTO, needs parsing and mapping
        const updatedCountryCodes = JSON.parse(updatedProfile.location || '[]');
        if (Array.isArray(updatedCountryCodes) && allCountriesData.length > 0) {
          // Map codes back to full country objects
          userCountries = updatedCountryCodes.map(code =>
            allCountriesData.find(country => country.cca2 === code)
          ).filter(Boolean); // Filter out nulls if a code wasn't found
        } else {
          userCountries = [];
        }
      } catch (e) {
        console.error("Failed to parse/map updated location:", e); // Updated error message slightly
        userCountries = [];
      }
      // Update initial bio state after successful save to reflect the new baseline
      initialUserBio = userBio; 

      // Add success feedback (e.g., toast notification)
      console.log("Profile saved successfully!");

    } catch (error) {
      console.error("Error saving profile:", error);
      saveError = error.message || "Failed to save profile. Please try again.";
    } finally {
      savingProfile = false;
    }
  }

  // Function to handle country changes and auto-save
  function handleCountryChange() {
    if (profileLoaded) { // Only save if initial load is complete
      console.log("Country changed, auto-saving profile...");
      saveProfile();
    }
  }

  // Function to change pages
  function goToPage(pageType, page) {
    if (pageType === 'posts' && page >= 1 && page <= totalPostPages) {
      postsPage = page;
    } else if (pageType === 'comments' && page >= 1 && page <= totalCommentPages) {
      commentsPage = page;
    }
    // Scroll to top of content when changing pages
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  
  // Function to generate pagination range
  function getPaginationRange(current, total) {
    if (total <= 5) {
      // If 5 or fewer pages, show all
      return Array.from({ length: total }, (_, i) => i + 1);
    }
    
    if (current <= 3) {
      // Near start: show first 5 pages
      return [1, 2, 3, 4, 5];
    }
    
    if (current >= total - 2) {
      // Near end: show last 5 pages
      return [total - 4, total - 3, total - 2, total - 1, total];
    }
    
    // In middle: show current and 2 on each side
    return [current - 2, current - 1, current, current + 1, current + 2];
  }

  // Additional function to check for comment content
  function logCommentData() {
    if (comments.length > 0) {
      console.log("Sample comment data:", comments[0]);
    }
  }

  // Additional function to debug post images
  function debugThreadImages() {
    if (threads.length > 0) {
      console.log("Sample thread data:", threads[0]);
      if (threads[0].mysteryObject) {
        console.log("Mystery object:", threads[0].mysteryObject);
      }
      if (threads[0].mediaFiles) {
        console.log("Media files:", threads[0].mediaFiles);
      }
    }
  }

  // Add function to get post titles for comments
  async function enrichCommentsWithPostTitles(comments) {
    const postIds = new Set();
    comments.forEach(comment => {
      if (comment.postId) postIds.add(comment.postId);
    });
    
    const postIdToTitle = {};
    
    // Fetch titles for each post ID
    for (const postId of postIds) {
      try {
        const response = await fetch(`${PUBLIC_API_URL}/api/posts/getForPostDetails/${postId}`);
        if (response.ok) {
          const postData = await response.json();
          postIdToTitle[postId] = postData.title || "Untitled Post";
        }
      } catch (error) {
        console.error(`Error fetching title for post ${postId}:`, error);
      }
    }
    
    // Add titles to comments
    return comments.map(comment => ({
      ...comment,
      postTitle: postIdToTitle[comment.postId] || "Unknown post"
    }));
  }

  async function toggleFollow() {
    if (!userName) return;

    followLoading = true;
    try {
      const endpoint = isFollowing
        ? `${PUBLIC_API_URL}/api/followed-users/unfollow/${userName}`
        : `${PUBLIC_API_URL}/api/followed-users/follow/${userName}`;

      const method = "POST";

      const response = await fetch(endpoint, {
        method,
        headers: {
          "Content-Type": "application/json",
        },
      });

      if (!response.ok) {
        throw new Error(
          `Failed to ${isFollowing ? "unfollow" : "follow"} user`,
        );
      }

      isFollowing = !isFollowing;
    } catch (error) {
      console.error(error);
    } finally {
      followLoading = false;
    }
  }

  // Fetch user data, threads and comments
  onMount(async () => {
    try {
      loadingThreads = true;
      loadingComments = true;
      
      // Fetch all countries data first
      try {
        const countriesResponse = await fetch('https://restcountries.com/v3.1/all?fields=name,flags,cca2');
        if (!countriesResponse.ok) throw new Error('Failed to fetch countries');
        const data = await countriesResponse.json();
        allCountriesData = data.sort((a, b) => a.name.common.localeCompare(b.name.common));
      } catch (err) {
        console.error('Error fetching countries in profile page:', err);
        // Handle error appropriately, maybe show a message, but continue loading profile
      }

      // First get the userId from the username
      const userResponse = await fetch(`${PUBLIC_API_URL}/api/auth/username/${userName}`);
      if (!userResponse.ok) {
        if (userResponse.status === 404) {
          userNotFound = true;
          throw new Error('User not found');
        }
        throw new Error(`Failed to fetch user: ${userResponse.status}`);
      }
      

      try {
        const followStatusResponse = await fetch(
          `${PUBLIC_API_URL}/api/followed-users/is-following/${userName}`,
        );
        if (followStatusResponse.ok) {
          const data = await followStatusResponse.json();
          isFollowing = data.isFollowing;
        }
      } catch (error) {
        console.error("Error checking follow status:", error);
      }

      // Fetch follower and following counts
      try {
        const followersResponse = await fetch(
          `${PUBLIC_API_URL}/api/followed-users/${userName}/followers-count`,
        );
        if (followersResponse.ok) {
          const data = await followersResponse.json();
          followerCount = data.followerCount || 0;
        }
      } catch (error) {
        console.error("Error fetching follower count:", error);
      }

      try {
        const followingResponse = await fetch(
          `${PUBLIC_API_URL}/api/followed-users/${userName}/following-count`,
        );
        if (followingResponse.ok) {
          const data = await followingResponse.json();
          followingCount = data.followingCount || 0;
        }
      } catch (error) {
        console.error("Error fetching following count:", error);
      }

      const userData = await userResponse.json();
      userId = userData.userId;
      
      // Fetch user profile data using the new endpoint
      try {
        // Use the username directly for the new endpoint
        const profileResponse = await fetch(`${PUBLIC_API_URL}/api/users/${userName}/profile`);
        if (profileResponse.ok) {
          const profileData = await profileResponse.json();
          // Use profilePictureUrl
          if (profileData.profilePictureUrl) {
            userImage = profileData.profilePictureUrl;
          }
          if (profileData.bio) {
            userBio = profileData.bio;
            initialUserBio = profileData.bio || ""; // Store initial bio
          }
          // Parse location JSON string and map codes to objects
          if (profileData.location) {
            try {
              const countryCodes = JSON.parse(profileData.location || '[]'); // Get ["TR", "US"]
              if (Array.isArray(countryCodes) && allCountriesData.length > 0) {
                // Map codes to full country objects
                userCountries = countryCodes.map(code =>
                   allCountriesData.find(country => country.cca2 === code)
                 ).filter(Boolean); // Filter out nulls if a code wasn't found
              } else {
                userCountries = [];
              }
            } catch (e) {
              console.error("Failed to parse/map location:", e);
              userCountries = [];
            }
          } else {
            userCountries = [];
          }
          // Store badges
          if (profileData.badges && Array.isArray(profileData.badges)) {
            userBadges = profileData.badges;
          }
        }
      } catch (profileError) {
        console.error("Error fetching profile data:", profileError);
      }
      
      // Fetch user's posts (threads)
      try {
        const postsResponse = await fetch(`${PUBLIC_API_URL}/api/auth/${userId}/posts`);
        if (!postsResponse.ok) throw new Error('Failed to fetch posts');
        const postsData = await postsResponse.json();

        console.log("User's posts raw data:", postsData);
        
        // Add direct API call for each post to get detailed data including images
        const enrichedPosts = [];
        for (const post of postsData) {
          try {
            const detailResponse = await fetch(`${PUBLIC_API_URL}/api/posts/getForPostDetails/${post.id}`);
            if (detailResponse.ok) {
              const detailData = await detailResponse.json();
              console.log(`Detailed data for post ${post.id}:`, detailData);
              enrichedPosts.push({...post, ...detailData});
            } else {
              enrichedPosts.push(post);
            }
          } catch (error) {
            console.error(`Error fetching details for post ${post.id}:`, error);
            enrichedPosts.push(post);
          }
        }
        
        // Process each thread to ensure consistent data structure
        const processedThreads = enrichedPosts.map(processThreadData);
        
        // Sort posts by most recent first
        threads = processedThreads.sort((a, b) => {
          const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
          const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
          return dateB - dateA;
        });
        
        // Update the thread store
        threadStore.set(threads);

        await updateTags();

        // Debug thread data
        debugThreadImages();
      } catch (error) {
        console.error("Error fetching posts:", error);
        threads = []; // Ensure threads is empty array on error
      } finally {
        loadingThreads = false;
      }
      
      // Fetch user's comments
      try {
        const commentsResponse = await fetch(`${PUBLIC_API_URL}/api/auth/${userId}/comments`);
        if (!commentsResponse.ok) throw new Error('Failed to fetch comments');
        const commentsData = await commentsResponse.json();
        
        // Sort comments by most recent first
        comments = commentsData.sort((a, b) => 
          new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
        );
        
        // Enrich comments with post titles
        comments = await enrichCommentsWithPostTitles(comments);
        
      } catch (error) {
        console.error("Error fetching comments:", error);
        comments = []; // Ensure comments is empty array on error
      } finally {
        loadingComments = false;
      }
      
    } catch (error) {
      console.error("Error fetching user data:", error);
       loadingThreads = false;
       loadingComments = false;
     }
 
     profileLoaded = true; // Mark initial load as complete
 
     // Add this at the end of the onMount function
    setTimeout(() => {
      logCommentData();
    }, 1000);
  });
</script>

<div class="min-h-screen flex justify-center bg-change dark:bg-dark shifting p-3 py-5">
  <div class="w-full max-w-7xl space-y-6">
    {#if userNotFound}
      <!-- User Not Found Card -->
      <Card.Root class="overflow-hidden bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800">
        <Card.Header class="p-6 text-center">
          <Card.Title class="text-2xl md:text-3xl font-bold text-red-600 dark:text-red-400">User Not Found</Card.Title>
          <Card.Description class="text-lg mt-4">The user "{userName}" does not exist or could not be loaded.</Card.Description>
          <Button variant="outline" class="mt-6" href="/">Return to Home</Button>
        </Card.Header>
      </Card.Root>
    {:else}
      <!-- Profile Header Card -->
      <Card.Root class="overflow-hidden bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800">
        <Card.Header class="p-6">
          <!-- Reorganized profile layout with centered alignment -->
          <div class="flex flex-col gap-5 items-center">
            <div class="flex flex-col md:flex-row gap-5 md:gap-8 items-center">
              <!-- Profile Image with upload button -->
              <div class="relative">
                <div class="w-24 h-24 sm:w-28 sm:h-28 rounded-full overflow-hidden 
                           ring-2 ring-primary/30 ring-offset-2 ring-offset-white dark:ring-offset-neutral-950
                           bg-gradient-to-br from-neutral-100 to-neutral-50 dark:from-neutral-800 dark:to-neutral-900
                           shadow-md transition-shadow duration-300">
                  {#if userImage}
                    <img 
                      src={userImage} 
                      alt={userName} 
                      class="w-full h-full object-cover" 
                    />
                  {:else}
                    <div class="flex items-center justify-center w-full h-full">
                      <span class="text-3xl sm:text-4xl font-medium bg-clip-text text-transparent bg-gradient-to-br from-primary to-primary/70">
                        {userName ? userName[0].toUpperCase() : '?'}
                      </span>
                    </div>
                  {/if}
                </div>
                
                {#if isCurrentUserProfile}
                  <label 
                    for="profile-image-upload" 
                    class="absolute bottom-1 right-1 cursor-pointer p-2 bg-primary dark:bg-neutral-950 text-white rounded-full 
                          shadow-md hover:bg-primary-dark dark:hover:bg-neutral-900 transition-colors duration-200"
                    title="Change profile picture"
                  >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"/>
                    <circle cx="12" cy="13" r="3"/>
                  </svg>
                  <input id="profile-image-upload" type="file" class="hidden" accept="image/*" on:change={handleImageUpload} />
                  </label>
                {/if}
              </div>

              <!-- User Info Column - Name and Location -->
              <div class="flex-1 space-y-3 text-center">
                <div class="flex items-center justify-center gap-3 flex-wrap">
                  <Card.Title class="text-2xl md:text-3xl font-bold"
                    >{userName}</Card.Title
                  >

                  {#if !isCurrentUserProfile && $activeUser != null}
                  <Button
                  variant="outline"
                  size="sm"
                  class={`text-xs px-4 rounded-full border-primary text-primary 
                         hover:bg-[#0F766E] border-2 hover:border-[#0F993C] hover:text-white transition
                  ${isFollowing ? 'bg-transparent' : 'bg-[#0F766E]'}`}
                  on:click={toggleFollow}
                  disabled={followLoading}
                >
                  {isFollowing ? "Following" : "Follow"}
                </Button>
                
                  {/if}
                </div>

                <div
                  class="flex justify-center gap-6 text-sm text-neutral-600 dark:text-neutral-400 mt-1"
                >
                  <div>
                    <span class="font-semibold">{followerCount}</span> Followers
                  </div>
                  <div>
                    <span class="font-semibold">{followingCount}</span> Following
                  </div>
                </div>

                <!-- Location info integrated here without border -->
                <div class="pt-1">
                  <div class="flex items-center justify-center gap-1.5 text-sm text-neutral-700 dark:text-neutral-300 mb-1.5">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-teal-500" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z" clip-rule="evenodd" />
                    </svg>
                    <span>Location</span>
                  </div>
                  <!-- Country selector with proper z-index and event handler -->
                  <div class="relative z-50">
                    <UserCountrySelector
                      bind:selectedCountries={userCountries}
                      isEditable={isCurrentUserProfile}
                      maxCountries={3}
                      on:change={handleCountryChange}
                    />
                  </div>
                </div>
              </div>
            </div>
            
            <!-- Bottom row: Bio and Badges in a two-column layout - Centered alignment -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 w-full">
              <!-- User Bio -->
              <div class="md:col-span-1 order-2 md:order-1">
                {#if isCurrentUserProfile}
                  <div class="relative bg-white dark:bg-neutral-950 rounded-md border border-neutral-200 dark:border-neutral-800">
                    <textarea
                      placeholder="Add your bio..."
                      class="w-full p-3 pb-9 border-0 rounded-md text-sm bg-transparent resize-none focus:ring-0 focus:outline-none text-center"
                      rows="3"
                      bind:value={userBio}
                    ></textarea>
                    {#if bioChanged}
                      <div class="absolute bottom-0 right-0 p-1.5">
                        <Button 
                          class="h-7 px-3 py-0 text-xs rounded-full bg-white hover:bg-white/50 dark:bg-black dark:hover:bg-black/50 text-black dark:text-white shadow-sm flex items-center justify-center gap-1"
                          size="sm"
                          on:click={saveProfile}
                          disabled={savingProfile}
                        >
                        <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
                        {savingProfile ? 'Saving...' : 'Save Bio'}
                        </Button>
                      </div>
                    {/if}
                    {#if saveError}
                      <p class="text-xs text-red-500 mt-1 text-center">{saveError}</p>
                    {/if}
                  </div>
                {:else if userBio}
                <div class="bg-white dark:bg-neutral-950 rounded-md border border-neutral-200 dark:border-neutral-800 p-3 h-full">
                  <h3 class="text-sm font-medium mb-2 text-neutral-700 dark:text-neutral-300 text-center">Bio</h3>
                    <p class="text-sm text-center">{userBio}</p>
                  </div>
                {:else}
                <div class="bg-white dark:bg-neutral-950 rounded-md border border-neutral-200 dark:border-neutral-800 p-3 h-full">
                  <h3 class="text-sm font-medium mb-2 text-neutral-700 dark:text-neutral-300 text-center">Bio</h3>
                  <p class="text-sm text-neutral-500 dark:text-neutral-400 italic text-center">No bio provided</p>
                  </div>
                {/if}
              </div>
              
              <!-- Badges -->
              <div class="md:col-span-1 order-1 md:order-2">
                <div class="bg-white dark:bg-neutral-950 rounded-md border border-neutral-200 dark:border-neutral-800 p-3 h-full">
                  <h3 class="text-sm font-medium mb-2 text-neutral-700 dark:text-neutral-300 flex items-center justify-center">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1.5 text-amber-500" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M6.267 3.455a3.066 3.066 0 001.745-.723 3.066 3.066 0 013.976 0 3.066 3.066 0 001.745.723 3.066 3.066 0 012.812 2.812c.051.643.304 1.254.723 1.745a3.066 3.066 0 010 3.976 3.066 3.066 0 00-.723 1.745 3.066 3.066 0 01-2.812 2.812 3.066 3.066 0 00-1.745.723 3.066 3.066 0 01-3.976 0 3.066 3.066 0 00-1.745-.723 3.066 3.066 0 01-2.812-2.812 3.066 3.066 0 00-.723-1.745 3.066 3.066 0 010-3.976 3.066 3.066 0 00.723-1.745 3.066 3.066 0 012.812-2.812zm7.44 5.252a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                    </svg>
                    Badges & Achievements
                  </h3>
                  <div class="flex flex-wrap gap-2 justify-center">
                    {#if userBadges.length > 0}
                      {#each userBadges as badge (badge.id)}
                        <div
                          class="text-xs border border-neutral-200 dark:border-neutral-700 rounded-full px-3 py-1 bg-neutral-50 dark:bg-neutral-900 text-neutral-700 dark:text-neutral-300 flex items-center gap-1"
                          title={badge.description}
                        >
                          {#if badge.iconUrl}
                            <img src={badge.iconUrl} alt={badge.name} class="h-3 w-3" />
                          {/if}
                          <span>{badge.name}</span>
                        </div>
                      {/each}
                    {:else}
                       <div class="text-xs text-neutral-500 italic">No badges earned yet.</div>
                    {/if}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Card.Header>
      </Card.Root>

      <!-- Completely redesigned tab component for posts/comments -->
      <div class="bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden">
        <Tabs defaultValue="posts" class="w-full">
          <!-- Fixed tab header styling -->
          <div class="px-4 pt-4 border-b border-neutral-200 dark:border-neutral-800">
            <div class="grid grid-cols-2 w-full bg-neutral-100 dark:bg-neutral-900 rounded-md overflow-hidden">
              <TabsTrigger
                value="posts"
                class="text-center rounded-none data-[state=active]:bg-white dark:data-[state=active]:bg-neutral-800 h-10 transition-all"
              >
                <div class="flex items-center justify-center gap-1.5">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2"/><path d="M3 9h18"/><path d="M3 15h18"/><path d="M9 3v18"/><path d="M15 3v18"/></svg>
                  <span>Posts ({threads.length})</span>
                </div>
              </TabsTrigger>
              <TabsTrigger 
                value="comments" 
                class="text-center rounded-none data-[state=active]:bg-white dark:data-[state=active]:bg-neutral-800 h-10 transition-all"
              >
                <div class="flex items-center justify-center gap-1.5">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 9a2 2 0 0 1-2 2H6l-4 4V4c0-1.1.9-2 2-2h8a2 2 0 0 1 2 2v5Z"/><path d="M18 9h2a2 2 0 0 1 2 2v11l-4-4h-6a2 2 0 0 1-2-2v-1"/></svg>
                  <span>Comments ({comments.length})</span>
                </div>
              </TabsTrigger>
            </div>
          </div>
          
          <!-- Posts Tab Content - Fixed image display -->
          <TabsContent value="posts" class="px-4 pb-4 pt-2">
            {#if loadingThreads}
              <div class="p-6 text-center">
                <div class="inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-primary border-r-transparent align-[-0.125em] motion-reduce:animate-[spin_1.5s_linear_infinite]"></div>
                <p class="mt-2 text-neutral-600 dark:text-neutral-400">Loading posts...</p>
              </div>
            {:else if threads.length === 0}
              <div class="p-10 text-center">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 mx-auto text-neutral-300 dark:text-neutral-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 13h6m-3-3v6m5 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <p class="mt-4 text-lg font-medium">No posts yet</p>
                <p class="mt-1 text-neutral-500 dark:text-neutral-400">This user hasn't created any posts</p>
              </div>
            {:else}
              <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 py-4">
                {#each paginatedThreads as thread (thread.id)}
                  <a href={`/thread/${thread.id}`} class="block h-full">
                    <div class="bg-white dark:bg-neutral-950 shadow-sm hover:shadow-md transition-shadow duration-200 rounded-lg border border-neutral-200 dark:border-neutral-800 overflow-hidden h-full flex flex-col">
                      <!-- Card header with image or placeholder - Fixed image display -->
                      <div class="aspect-video relative bg-neutral-100 dark:bg-neutral-900 overflow-hidden">
                        <!-- Debug output to see media files data -->
                        <!-- {JSON.stringify(thread.mediaFiles)} -->
                        
                        {#if thread.mediaFiles && thread.mediaFiles.length > 0 && thread.mediaFiles[0].url}
                          <!-- Use onError to provide fallback when image fails to load -->
                          <img 
                            src={thread.mediaFiles[0].url} 
                            alt={thread.title}
                            class="w-full h-full object-cover"
                            on:error={(e) => {
                              console.log(`Image error for ${thread.id}:`, e);
                              e.target.src = "https://via.placeholder.com/300x200?text=No+Image";
                            }}
                          />
                        {:else if thread.mysteryObject && thread.mysteryObject.imageUrl}
                          <img 
                            src={thread.mysteryObject.imageUrl} 
                            alt={thread.title}
                            class="w-full h-full object-cover"
                            on:error={(e) => {
                              e.target.src = "https://via.placeholder.com/300x200?text=No+Image";
                            }}
                          />
                        {:else}
                          <div class="flex items-center justify-center h-full">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-neutral-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 13h6m-3-3v6m5 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                            </svg>
                          </div>
                        {/if}
                        
                        <!-- Solved badge if applicable -->
                        {#if thread.solved}
                        <div class="absolute top-2 right-2 bg-emerald-500 text-white text-xs font-medium px-2 py-1 rounded-full flex items-center">
                          <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-1" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                            </svg>
                            Resolved
                          </div>
                        {/if}
                      </div>
                      
                      <!-- Card content -->
                      <div class="p-4 flex-1 flex flex-col">
                        <h3 class="font-semibold text-neutral-900 dark:text-neutral-100 line-clamp-2 mb-1">{thread.title || "Untitled Post"}</h3>
                        <p class="text-sm text-neutral-600 dark:text-neutral-400 line-clamp-2 mb-auto">{thread.description || ""}</p>

                        <!-- Tags -->
                        {#if thread.tags && thread.tags.length > 0}
                          <div class="flex flex-wrap gap-1 mt-3">
                            {#each thread.tags.slice(0, 3) as tag}
                              <span class="inline-block bg-neutral-100 dark:bg-neutral-900 text-neutral-800 dark:text-neutral-300 px-2 py-0.5 rounded-full text-xs">
                                {tagMap.get(tag) || tag}
                              </span>
                            {/each}
                            {#if thread.tags.length > 3}
                              <span class="inline-block bg-neutral-100 dark:bg-neutral-900 text-neutral-500 px-2 py-0.5 rounded-full text-xs">
                                +{thread.tags.length - 3}
                              </span>
                            {/if}
                          </div>
                        {/if}
                        
                        <!-- Card footer with metadata -->
                        <div class="flex justify-between items-center mt-3 pt-3 border-t border-neutral-100 dark:border-neutral-800 text-xs text-neutral-500">
                          <div>
                            {formatDate(thread.createdAt)}
                          </div>
                          <div class="flex items-center">
                            <span class="flex items-center mr-2">
                              <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1" viewBox="0 0 20 20" fill="currentColor">
                                <path d="M2 10.5a1.5 1.5 0 113 0v6a1.5 1.5 0 01-3 0v-6zM6 10.333v5.43a2 2 0 001.106 1.79l.05.025A4 4 0 008.943 18h5.416a2 2 0 001.962-1.608l1.2-6A2 2 0 0015.56 8H12V4a2 2 0 00-2-2 1 1 0 00-1 1v.667a4 4 0 01-.8 2.4L6.8 7.933a4 4 0 00-.8 2.4z" />
                              </svg>
                              {thread.upvotes || 0}
                            </span>
                            <span class="flex items-center">
                              <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1" viewBox="0 0 20 20" fill="currentColor">
                                <path fill-rule="evenodd" d="M18 5v8a2 2 0 01-2 2h-5l-5 4v-4H4a2 2 0 01-2-2V5a2 2 0 012-2h12a2 2 0 012 2zM7 8H5v2h2V8zm2 0h2v2H9V8zm6 0h-2v2h2V8z" clip-rule="evenodd" />
                              </svg>
                              {thread.commentCount || 0}
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </a>
                {/each}
              </div>
              
              <!-- Posts Pagination -->
              {#if totalPostPages > 1}
                <div class="flex justify-center mt-4">
                  <div class="flex items-center gap-1">
                    <!-- First page button -->
                    <Button
                      variant="outline"
                      size="sm"
                      class="h-8 w-8 p-0 rounded-md"
                      disabled={postsPage === 1}
                      on:click={() => goToPage('posts', 1)}
                    >
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M15.707 15.707a1 1 0 01-1.414 0l-5-5a1 1 0 010-1.414l5-5a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 010 1.414zm-6 0a1 1 0 01-1.414 0l-5-5a1 1 0 010-1.414l5-5a1 1 0 011.414 1.414L5.414 10l4.293 4.293a1 1 0 010 1.414z" clip-rule="evenodd" />
                      </svg>
                    </Button>
                    
                    <!-- Previous page button -->
                    <Button 
                      variant="outline" 
                      size="sm" 
                      class="h-8 w-8 p-0 rounded-md"
                      disabled={postsPage === 1}
                      on:click={() => goToPage('posts', postsPage - 1)}
                    >
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M12.707 5.293a1 1 0 010 1.414L9.414 10l3.293 3.293a1 1 0 01-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd" />
                      </svg>
                    </Button>
                    
                    <!-- Page numbers -->
                    {#each getPaginationRange(postsPage, totalPostPages) as page}
                      <Button
                      variant={page === postsPage ? 'default' : 'outline'}
                      size="sm" 
                      class="h-8 w-8 p-0 {page === postsPage ? 'bg-teal-600 hover:bg-teal-700 text-white' : ''} rounded-md"
                      on:click={() => goToPage('posts', page)}
                      >
                        {page}
                      </Button>
                    {/each}
                    
                    <!-- Next page button -->
                    <Button 
                      variant="outline" 
                      size="sm" 
                      class="h-8 w-8 p-0 rounded-md"
                      disabled={postsPage === totalPostPages}
                      on:click={() => goToPage('posts', postsPage + 1)}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd" />
                      </svg>
                    </Button>
                    
                    <!-- Last page button -->
                    <Button 
                      variant="outline" 
                      size="sm" 
                      class="h-8 w-8 p-0 rounded-md"
                      disabled={postsPage === totalPostPages}
                      on:click={() => goToPage('posts', totalPostPages)}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M4.293 15.707a1 1 0 010-1.414L8.586 10 4.293 6.707a1 1 0 011.414-1.414l5 5a1 1 0 010 1.414l-5 5a1 1 0 01-1.414 0zM10.293 15.707a1 1 0 010-1.414L14.586 10l-4.293-3.293a1 1 0 011.414-1.414l5 5a1 1 0 010 1.414l-5 5a1 1 0 01-1.414 0z" clip-rule="evenodd" />
                      </svg>
                    </Button>
                  </div>
                </div>
              {/if}
            {/if}
          </TabsContent>
          
          <!-- Comments Tab Content -->
          <TabsContent value="comments" class="px-4 pb-4 pt-2">
            {#if loadingComments}
              <div class="p-6 text-center">
                <div class="inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-primary border-r-transparent align-[-0.125em] motion-reduce:animate-[spin_1.5s_linear_infinite]"></div>
                <p class="mt-2 text-neutral-600 dark:text-neutral-400">Loading comments...</p>
              </div>
            {:else if comments.length === 0}
              <div class="p-10 text-center">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 mx-auto text-neutral-300 dark:text-neutral-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
                </svg>
                <p class="mt-4 text-lg font-medium">No comments yet</p>
                <p class="mt-1 text-neutral-500 dark:text-neutral-400">This user hasn't posted any comments</p>
              </div>
            {:else}
              <div class="divide-y divide-neutral-100 dark:divide-neutral-800 py-4">
                {#each paginatedComments as comment (comment.id)}
                  <div class="py-4 first:pt-0 last:pb-0">
                    <a href={`/thread/${comment.postId}`} class="block hover:bg-neutral-50 dark:hover:bg-neutral-900 rounded-lg p-3 transition-colors">
                      <div class="flex items-start gap-3">
                        {#if comment.commentType}
                          <div class="flex-shrink-0 mt-1">
                            {#if comment.commentType === 'QUESTION'}
                              <div class="w-8 h-8 rounded-full bg-amber-100 dark:bg-amber-900/30 flex items-center justify-center">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-amber-600 dark:text-amber-400" viewBox="0 0 20 20" fill="currentColor">
                                  <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
                                </svg>
                              </div>
                            {:else if comment.commentType === 'SUGGESTION'}
                              <div class="w-8 h-8 rounded-full bg-emerald-100 dark:bg-emerald-900/30 flex items-center justify-center">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-emerald-600 dark:text-emerald-400" viewBox="0 0 20 20" fill="currentColor">
                                  <path d="M11 3a1 1 0 10-2 0v1a1 1 0 102 0V3zM15.657 5.757a1 1 0 00-1.414-1.414l-.707.707a1 1 0 001.414 1.414l.707-.707zM18 10a1 1 0 01-1 1h-1a1 1 0 110-2h1a1 1 0 011 1zM5.05 6.464A1 1 0 106.464 5.05l-.707-.707a1 1 0 00-1.414 1.414l.707.707zM5 10a1 1 0 01-1 1H3a1 1 0 110-2h1a1 0 011 1zM8 16v-1h4v1a2 2 0 11-4 0zM12 14c.015-.34.208-.646.477-.859a4 4 0 10-4.954 0c.27.213.462.519.476.859h4.002z" />
                                </svg>
                              </div>
                            {:else if comment.commentType === 'STORY'}
                              <div class="w-8 h-8 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-blue-600 dark:text-blue-400" viewBox="0 0 20 20" fill="currentColor">
                                  <path d="M9 4.804A7.968 7.968 0 005.5 4c-1.255 0-2.443.29-3.5.804v10A7.969 7.969 0 015.5 14c1.669 0 3.218.51 4.5 1.385A7.962 7.962 0 0114.5 14c1.255 0 2.443.29 3.5.804v-10A7.968 7.968 0 0014.5 4c-1.255 0-2.443.29-3.5.804V12a1 1 0 11-2 0V4.804z" />
                                </svg>
                              </div>
                            {:else}
                              <div class="w-8 h-8 rounded-full bg-neutral-100 dark:bg-neutral-900 flex items-center justify-center">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-neutral-600 dark:text-neutral-400" viewBox="0 0 20 20" fill="currentColor">
                                  <path fill-rule="evenodd" d="M18 5v8a2 2 0 01-2 2h-5l-5 4v-4H4a2 2 0 01-2-2V5a2 2 0 012-2h12a2 2 0 012 2zM7 8H5v2h2V8zm2 0h2v2H9V8zm6 0h-2v2h2V8z" clip-rule="evenodd" />
                                </svg>
                              </div>
                            {/if}
                          </div>
                        {/if}
                        
                        <div class="flex-1 min-w-0">
                          <!-- Comment Content -->
                          <div class="mb-2 text-neutral-900 dark:text-neutral-100">
                            <!-- Debug output when content is missing -->
                            {#if !comment.content && comment.comment}
                              <p class="whitespace-pre-wrap break-words">{comment.comment}</p>
                            {:else}
                              <p class="whitespace-pre-wrap break-words">{comment.content || "No content"}</p>
                            {/if}
                          </div>
                          
                          <!-- Media Files (if any) -->
                          {#if comment.mediaFiles && comment.mediaFiles.length > 0}
                            <div class="flex flex-wrap gap-2 mt-2">
                              {#each comment.mediaFiles as media (media.id)}
                                {#if media.contentType && media.contentType.startsWith('image/')}
                                  <div class="relative w-16 h-16 rounded overflow-hidden border border-neutral-200 dark:border-neutral-800">
                                    <img src={media.url} alt="Comment attachment" class="w-full h-full object-cover" />
                                  </div>
                                {/if}
                              {/each}
                            </div>
                          {/if}
                          
                          <!-- Context and metadata - Fixed post title -->
                          <div class="mt-2 flex flex-col sm:flex-row sm:items-center text-xs text-neutral-500 dark:text-neutral-400 gap-2 sm:gap-4">
                            <!-- Post this comment belongs to - with truncated title -->
                            <div class="flex items-center">
                              <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-1" viewBox="0 0 20 20" fill="currentColor">
                                <path fill-rule="evenodd" d="M12.586 4.586a2 2 0 112.828 2.828l-3 3a2 2 0 01-2.828 0 1 1 0 00-1.414 1.414 4 4 0 005.656 0l3-3a4 4 0 00-5.656-5.656l-1.5 1.5a1 1 0 101.414 1.414l1.5-1.5zm-5 5a2 2 0 012.828 0 1 1 0 101.414-1.414 4 4 0 00-5.656 0l-3 3a4 4 0 105.656 5.656l1.5-1.5a1 1 0 10-1.414-1.414l-1.5 1.5a2 2 0 11-2.828-2.828l3-3z" clip-rule="evenodd" />
                              </svg>
                              <span class="truncate max-w-[180px] inline-block font-medium text-neutral-700 dark:text-neutral-300">
                                {comment.postTitle || "Unknown post"}
                              </span>
                            </div>
                            
                            <!-- Date -->
                            <div class="flex items-center">
                              <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-1" viewBox="0 0 20 20" fill="currentColor">
                                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clip-rule="evenodd" />
                              </svg>
                              <span>{formatDate(comment.createdAt)}</span>
                            </div>
                            
                            <!-- Comment stats -->
                            <div class="flex items-center gap-2">
                              <span class="flex items-center">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-1" viewBox="0 0 20 20" fill="currentColor">
                                  <path d="M2 10.5a1.5 1.5 0 113 0v6a1.5 1.5 0 01-3 0v-6zM6 10.333v5.43a2 2 0 001.106 1.79l.05.025A4 4 0 008.943 18h5.416a2 2 0 001.962-1.608l1.2-6A2 2 0 0015.56 8H12V4a2 2 0 00-2-2 1 1 0 00-1 1v.667a4 4 0 01-.8 2.4L6.8 7.933a4 4 0 00-.8 2.4z" />
                                </svg>
                                {comment.upvotes || 0}
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </a>
                  </div>
                {/each}
              </div>
              
              <!-- Comments Pagination -->
              {#if totalCommentPages > 1}
                <div class="flex justify-center mt-4">
                  <div class="flex items-center gap-1">
                    <!-- First page button -->
                    <Button 
                      variant="outline" 
                      size="sm" 
                      class="h-8 w-8 p-0 rounded-md"
                      disabled={commentsPage === 1}
                      on:click={() => goToPage('comments', 1)}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M15.707 15.707a1 1 0 01-1.414 0l-5-5a1 1 0 010-1.414l5-5a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 010 1.414zm-6 0a1 1 0 01-1.414 0l-5-5a1 1 0 010-1.414l5-5a1 1 0 011.414 1.414L5.414 10l4.293 4.293a1 1 0 010 1.414z" clip-rule="evenodd" />
                      </svg>
                    </Button>
                    
                    <!-- Previous page button -->
                    <Button 
                      variant="outline" 
                      size="sm" 
                      class="h-8 w-8 p-0 rounded-md"
                      disabled={commentsPage === 1}
                      on:click={() => goToPage('comments', commentsPage - 1)}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M12.707 5.293a1 1 0 010 1.414L9.414 10l3.293 3.293a1 1 0 01-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd" />
                      </svg>
                    </Button>
                    
                    <!-- Page numbers -->
                    {#each getPaginationRange(commentsPage, totalCommentPages) as page}
                      <Button 
                        variant={page === commentsPage ? 'default' : 'outline'}
                        size="sm" 
                        class="h-8 w-8 p-0 {page === commentsPage ? 'bg-teal-600 hover:bg-teal-700 text-white' : ''} rounded-md"
                        on:click={() => goToPage('comments', page)}
                      >
                        {page}
                      </Button>
                    {/each}
                    
                    <!-- Next page button -->
                    <Button 
                      variant="outline" 
                      size="sm" 
                      class="h-8 w-8 p-0 rounded-md"
                      disabled={commentsPage === totalCommentPages}
                      on:click={() => goToPage('comments', commentsPage + 1)}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd" />
                      </svg>
                    </Button>
                    
                    <!-- Last page button -->
                    <Button 
                      variant="outline" 
                      size="sm" 
                      class="h-8 w-8 p-0 rounded-md"
                      disabled={commentsPage === totalCommentPages}
                      on:click={() => goToPage('comments', totalCommentPages)}
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M4.293 15.707a1 1 0 010-1.414L8.586 10 4.293 6.707a1 1 0 011.414-1.414l5 5a1 1 0 010 1.414l-5 5a1 1 0 01-1.414 0zM10.293 15.707a1 1 0 010-1.414L14.586 10l-4.293-3.293a1 1 0 011.414-1.414l5 5a1 1 0 010 1.414l-5 5a1 1 0 01-1.414 0z" clip-rule="evenodd" />
                      </svg>
                    </Button>
                  </div>
                </div>
              {/if}
            {/if}
          </TabsContent>
        </Tabs>
      </div>
    {/if}
  </div>
</div>

<style>
  /* Remove the old animation styles */
  /* Ensure text gradient works in different browsers */
  .text-transparent {
    -webkit-background-clip: text;
    background-clip: text;
  }
</style>
