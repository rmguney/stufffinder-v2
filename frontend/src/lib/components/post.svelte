<script>
  import * as Card from "$lib/components/ui/card";
  import { onMount } from "svelte";
  import { writable } from "svelte/store";
  import { activeUser } from "../../userStore";
  import { Button } from "$lib/components/ui/button";
  import { Textarea } from "$lib/components/ui/textarea";
  import { Separator } from "$lib/components/ui/separator";
  import { PUBLIC_API_URL } from "$env/static/public";
  import MysteryObjectSubParts from "./mysteryObjectSubParts.svelte";
  import { getMysteryObjectWithSubParts } from "$lib/utils/mysteryObjectUtils.js";
  import { createEventDispatcher } from 'svelte';
  import { getAuthHeader } from '$lib/utils/auth'; 

  export let id = '';
  export let title = '';
  export let description = '';
  export let tags = [];
  export let imageSrc = ''; // Keep for backward compatibility
  export let mediaFiles = []; // New prop for multiple media files
  export let solved = false;
  export let resolution = null;
  export let variant = "default"; // "default" or "thumb"
  export let mysteryObject = null;  // Changed from object with defaults to null
  let mysteryObjectSubParts = mysteryObject?.subParts || [];
  export let postedBy = '';
  //export let postedDate = '';
  export let upvotes = 0;
  export let downvotes = 0;
  export let userUpvoted = false;
  export let userDownvoted = false;
  export let createdAt = '';
  export let updatedAt = '';
  export let commentCount = 0;
  export let currentUser = null;
  export let contributingCommentObjects = []; // New prop to receive actual comment objects

  let tagDetails = writable([]);
  let currentMediaIndex = 0; // Track the current displayed media in carousel
  let isLoadingSubParts = false;
  let subPartsError = null;
  let isFollowing = false;
  let followerCount = 0;


  let colorNames = new Map(); // Cache for color names to avoid duplicate API calls
  let colorHexes = new Map(); // Cache for color hex values from names
  let isLoadingColorNames = new Set(); // Track which colors are currently loading
  let isLoadingColorHexes = new Set(); // Track which color names are being converted to hex

  // Updated to include any other possible color name related properties
  const skipProperties = ['id', 'images', 'imageUrl', 'description', 'subParts', 'parent', 'colorName', 'color_name', 'color_label'];

  // Explicitly check if there's any color name field being generated separately from color
  function hasColorField(obj) {
    return Object.keys(obj).some(key => 
      key === 'color' || 
      key === 'colorName' || 
      key === 'color_name' || 
      key.toLowerCase().includes('colorname')
    );
  }

  const fetchTagDetails = async () => {
    if (!tags.length) {
      return;
    }
    try {
      let fetchedTags = await Promise.all(tags.map(async (qcode) => {
        const response = await fetch(`https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${qcode}&format=json&languages=en&props=labels|descriptions&origin=*`);
        const data = await response.json();
        const entity = data.entities[qcode];
        return {
          label: entity.labels?.en?.value || 'Unknown label',
          description: entity.descriptions?.en?.value || 'No description',
          id: qcode
        };
      }));
      tagDetails.set(fetchedTags);
    } catch (error) {
      console.error("Failed to fetch tag details:", error);
    }
  };
  
  // Resolution modal state
  let showResolutionModal = false;
  let resolutionDescription = "";
  let selectedComments = [];
  let resolutionError = null;
  
  // Function to open resolution modal
  const openResolutionModal = () => {
    if (currentUser !== postedBy) return;
    showResolutionModal = true;
    resolutionDescription = resolution?.description || "";
    selectedComments = resolution?.contributingComments || [];
  };
  
  // Function to resolve a post
  const resolvePost = async () => {
    if (currentUser !== postedBy) return;
    if (!resolutionDescription.trim()) {
      resolutionError = "Please provide a resolution description";
      return;
    }
    
    try {
      resolutionError = null;
      const resolutionData = {
        description: resolutionDescription,
        contributingCommentIds: selectedComments
      };

      const response = await fetch(`${PUBLIC_API_URL}/api/posts/${id}/resolve`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(resolutionData)
      });

      if (!response.ok) throw new Error('Failed to resolve post');
      const data = await response.json();
      
      // Update local state
      solved = true;
      resolution = {
        description: resolutionDescription,
        contributingComments: selectedComments,
        resolvedAt: new Date().toISOString()
      };
      
      // Close modal
      showResolutionModal = false;
      
      // Refresh the page to show updated state
      document.dispatchEvent(new CustomEvent('refreshPost', {
        bubbles: true,
        detail: { postId: id }
      }));
    } catch (error) {
      console.error("Error resolving post:", error);
      resolutionError = error.message || "Failed to resolve post";
    }
  };
  
  // Function to unresolve a post
  const unresolvePost = async () => {
    if (currentUser !== postedBy) return;
    
    try {
      const response = await fetch(`${PUBLIC_API_URL}/api/posts/${id}/unresolve`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          ...getAuthHeader()  // Add authorization headers
        }
      });

      if (!response.ok) throw new Error('Failed to unresolve post');
      const data = await response.json();
      
      // Update local state
      solved = false;
      resolution = null;
      
      // Refresh the page to show updated state
      document.dispatchEvent(new CustomEvent('refreshPost', {
        bubbles: true,
        detail: { postId: id }
      }));
    } catch (error) {
      console.error("Error unresolving post:", error);
    }
  };
  
  // Toggle comment selection for resolution
  const toggleCommentSelection = (commentId) => {
    if (selectedComments.includes(commentId)) {
      selectedComments = selectedComments.filter(id => id !== commentId);
    } else {
      selectedComments = [...selectedComments, commentId];
    }
  };

  // More robust date formatting
  const formatDate = (isoDate) => {
    console.log('isoDate: ', isoDate);
    if (!isoDate) {
      console.warn("Warning: Empty date value received");
      return "";
    }

    try {
      // First check if this is already an ISO string
      const date = new Date(isoDate);

      if (isNaN(date.getTime())) {
        console.warn("Warning: Invalid date format:", isoDate);
        return "";
      }

      const timeOptions = { hour: "numeric", minute: "numeric", hour12: false };
      const timeString = new Intl.DateTimeFormat("en-US", timeOptions).format(date);

      const day = date.getDate();
      const shortMonth = date.toLocaleString("en-US", { month: "short" });
      const fullYear = date.getFullYear();

      return `${timeString}, ${day} ${shortMonth} ${fullYear}`;
    } catch (error) {
      console.error("Date formatting error:", error, "for date:", isoDate);
      return "";
    }
  };

  // Debug logs to verify the date values
  $: if (variant === "thumb") {
    console.log(`Post component (${id}): variant=${variant}, createdAt=${createdAt}, formatted=`, formatDate(createdAt));
  }

  function handleImageError(event) {
    const svgIcon = `
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="white" class="size-10 bg-black">
      <path stroke-linecap="round" stroke-linejoin="round" d="m15.75 10.5 4.72-4.72a.75.75 0 0 1 1.28.53v11.38a.75.75 0 0 1-1.28.53l-4.72-4.72M4.5 18.75h9a2.25 2.25 0 0 0 2.25-2.25v-9a2.25 2.25 0 0 0-2.25-2.25h-9A2.25 2.25 0 0 0 2.25 7.5v9a2.25 2.25 0 0 0 2.25 2.25Z" />
    </svg>
    `;

    event.target.src = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svgIcon)}`;
    event.target.style.backgroundColor = '#f0f0f0';
    event.target.alt = 'Image not available';
  }

  function handleMediaError(event) {
    const svgIcon = `
    <svg xmlns="http://www.w3.org/20000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="white" class="size-10 bg-black">
      <path stroke-linecap="round" stroke-linejoin="round" d="m15.75 10.5 4.72-4.72a.75.75 0 0 1 1.28.53v11.38a.75.75 0 0 1-1.28.53l-4.72-4.72M4.5 18.75h9a2.25 2.25 0 0 0 2.25-2.25v-9a2.25 2.25 0 0 0-2.25-2.25h-9A2.25 2.25 0 0 0 2.25 7.5v9a2.25 2.25 0 0 0 2.25 2.25Z" />
    </svg>
    `;

    event.target.src = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svgIcon)}`;
    event.target.style.backgroundColor = '#000';
    event.target.alt = 'Media not available';
  }

  // Function to determine if a string is a valid hex color - improved regex
  function isHexColor(str) {
    if (!str || typeof str !== 'string') return false;
    return /^#([0-9A-Fa-f]{3}){1,2}$/.test(str);
  }

  // Function to ensure color values are properly formatted
  function formatColorForDisplay(color) {
    console.log('Formatting color for display:', color);
    if (!color) return '#cccccc'; // Default fallback color

    if (typeof color === 'string') {
      // If it's already a valid hex color, return it
      if (isHexColor(color)) return color;

      // Try to add # if missing
      if (!color.startsWith('#') && /^([0-9A-Fa-f]{3}){1,2}$/.test(color)) {
        return '#' + color;
      }

      // If we've already converted this color name to hex, use the cached value
      if (colorHexes.has(color.toLowerCase())) {
        return colorHexes.get(color.toLowerCase());
      }

      // For immediate display, use a fallback color and fetch the proper one asynchronously
      fetchColorHexFromName(color); // This will update the UI once it resolves
      return color; // Default fallback while waiting for API
    }

    // Return a fallback if nothing else works
    return '#cccccc';
  }

  // Function to fetch color name from TheColorAPI - similar to colorPicker component
  async function fetchColorName(hexColor) {
    if (!isHexColor(hexColor)) {
      console.warn('Invalid hex color provided to fetchColorName:', hexColor);
      return hexColor; // Return the original value if not a valid hex
    }

    // Return from cache if already fetched
    if (colorNames.has(hexColor)) {
      return colorNames.get(hexColor);
    }

    isLoadingColorNames.add(hexColor);
    isLoadingColorNames = isLoadingColorNames; // Trigger reactivity

    // Remove the # character from hex colors
    const cleanHex = hexColor.replace('#', '');

    try {
      console.log('Fetching color name for:', hexColor);
    const response = await fetch(`https://www.thecolorapi.com/id?hex=${cleanHex}`);
      if (!response.ok) {
        throw new Error('Failed to fetch color name');
      }
      const data = await response.json();
      console.log('Color API response:', data);
      const colorName = data.name?.value || 'Unknown color';

      // Update cache
      colorNames.set(hexColor, colorName);

      // Remove from loading
      isLoadingColorNames.delete(hexColor);
      isLoadingColorNames = isLoadingColorNames; // Trigger reactivity

      return colorName;
    } catch (error) {
      console.error('Error fetching color name:', error);

      // Remove from loading
      isLoadingColorNames.delete(hexColor);
      isLoadingColorNames = isLoadingColorNames; // Trigger reactivity

      return 'Unknown color';
    }
  }

  // Function to fetch hex value for a color name, similar to colorPicker component
  async function fetchColorHexFromName(colorName) {
    if (!colorName || typeof colorName !== 'string') return null;

    const lowerColorName = colorName.toLowerCase();

    // Return from cache if already fetched
    if (colorHexes.has(lowerColorName)) {
      return colorHexes.get(lowerColorName);
    }

    // Don't fetch if already loading
    if (isLoadingColorHexes.has(lowerColorName)) return null;

    isLoadingColorHexes.add(lowerColorName);
    isLoadingColorHexes = isLoadingColorHexes; // Trigger reactivity

    try {
      console.log('Fetching hex for color name:', colorName);
      const response = await fetch(`https://www.thecolorapi.com/id?name=${encodeURIComponent(colorName)}`);
      if (!response.ok) {
        throw new Error('Failed to fetch color hex');
      }
      const data = await response.json();
      console.log('Color API response for name:', data);
      const hexValue = data.hex?.value || '#cccccc';

      // Update cache
      colorHexes.set(lowerColorName, hexValue);

      // Remove from loading
      isLoadingColorHexes.delete(lowerColorName);
      isLoadingColorHexes = isLoadingColorHexes; // Trigger reactivity

      return hexValue;
    } catch (error) {
      console.error('Error fetching color hex:', error);

      // Remove from loading
      isLoadingColorHexes.delete(lowerColorName);
      isLoadingColorHexes = isLoadingColorHexes; // Trigger reactivity

      return '#cccccc';
    }
  }

  $: activeUser.subscribe((value) => {
    currentUser = value;
  });

  // Update mysteryObjectSubParts whenever mysteryObject changes
  $: if (mysteryObject && mysteryObject.subParts) {
    mysteryObjectSubParts = mysteryObject.subParts;
  }

  onMount(async () => {
    fetchTagDetails();
     
     if(currentUser){
      try {
        
        const [statusResponse,countResponse] = await Promise.all([
          fetch(`${PUBLIC_API_URL}/api/followed-posts/${id}/is-following`, {
            headers: {
              "Content-Type": "application/json",
              ...getAuthHeader(),
            },
          }),
          fetch(`${PUBLIC_API_URL}/api/followed-posts/${id}/followers-count`, {
            headers: {
              "Content-Type": "application/json"
            },
          }),
        ]);
  
        if (!statusResponse.ok || !countResponse.ok)
          throw new Error("Failed to fetch follow data");
  
        const statusData = await statusResponse.json();
        const countData = await countResponse.json();
  
        isFollowing = statusData.isFollowing;
        followerCount = countData.followerCount;
      } catch (error) {
        console.error("Error fetching follow info:", error);
      }
     }
     else{
      try {
        
        const [countResponse] = await Promise.all([
          fetch(`${PUBLIC_API_URL}/api/followed-posts/${id}/followers-count`, {
            headers: {
              "Content-Type": "application/json"
            },
          }),
        ]);
  
        if (!countResponse.ok)
          throw new Error("Failed to fetch follow data");
  
        const countData = await countResponse.json();
  
        isFollowing = false;
        followerCount = countData.followerCount;
      } catch (error) {
        console.error("Error fetching follow info:", error);
      }
     }
     

   
  });

  // No need to load sub-parts separately since they're already included in the mysteryObject
  
  // Handle updates from the MysteryObjectSubParts component
  function handleSubPartsUpdate(event) {
    mysteryObjectSubParts = event.detail.subParts;
  }

  const handleVote = async (isUpvote) => {
    if (!currentUser) return; // Must be logged in to vote
    
    try {
      const response = await fetch(`${PUBLIC_API_URL}/api/posts/${isUpvote ? 'upvote' : 'downvote'}/${id}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        }
      });

      if (!response.ok) throw new Error('Failed to vote');
      const data = await response.json();
      
      // Update local state
      upvotes = data.upvotes;
      downvotes = data.downvotes;
      userUpvoted = isUpvote && !userUpvoted;
      userDownvoted = !isUpvote && !userDownvoted;
      
    } catch (error) {
      console.error('Error voting:', error);
    }
  };

  // Add a debug log when postedBy changes
  $: {
    if (variant === 'thread') {
      // console.log('Posted by:', postedBy);
    }
  }

  // Convert single imageSrc to mediaFiles array for backward compatibility
  $: {
    if (imageSrc && mediaFiles.length === 0) {
      mediaFiles = [{
        type: 'image',
        url: imageSrc,
        name: 'Image'
      }];
    }
  }
  
  // For thumbnail view, get first image if available
  $: thumbnailImage = mediaFiles.find(file => file.type === 'image')?.url || 
                     (mediaFiles.length > 0 ? mediaFiles[0].url : '');
  
  // Functions to control the carousel
  const nextMedia = () => {
    if (mediaFiles.length > 1) {
      currentMediaIndex = (currentMediaIndex + 1) % mediaFiles.length;
    }
  };
  
  const prevMedia = () => {
    if (mediaFiles.length > 1) {
      currentMediaIndex = (currentMediaIndex - 1 + mediaFiles.length) % mediaFiles.length;
    }
  };

  const dispatch = createEventDispatcher();

  // Add state for the tag tooltip
  let hoveredTag = null;
  let tagImageLoading = false;
  let tagImage = null;
  let tooltipVisible = false;
  let tooltipPosition = { x: 0, y: 0 };
  
  // Function to fetch Wikidata image for a tag
  async function fetchTagImage(tag) {
    if (!tag || !tag.id) return null;
    
    try {
      tagImageLoading = true;
      
      // First fetch the Wikidata entity to get the image property
      const entityResponse = await fetch(
        `https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${tag.id}&format=json&props=claims&origin=*`
      );
      
      if (!entityResponse.ok) {
        throw new Error('Failed to fetch entity data');
      }
      
      const entityData = await entityResponse.json();
      const entity = entityData.entities[tag.id];
      
      // Check if entity has image (P18) property
      if (entity && 
          entity.claims && 
          entity.claims.P18 && 
          entity.claims.P18.length > 0) {
        
        // Get the image filename
        const imageFilename = entity.claims.P18[0].mainsnak.datavalue.value;
        
        // Convert to URL using Wikimedia Commons
        // We need to MD5 hash the filename for the URL path, but for simplicity
        // we'll use the direct filename which works for most common cases
        const encodedFilename = encodeURIComponent(imageFilename.replace(/ /g, '_'));
        
        // Return the Commons thumbnail URL
        return `https://commons.wikimedia.org/wiki/Special:FilePath/${encodedFilename}?width=300`;
      }
      
      return null;
    } catch (error) {
      console.error("Error fetching tag image:", error);
      return null;
    } finally {
      tagImageLoading = false;
    }
  }
  

  async function toggleFollow() {
    if (!currentUser) return;

    const endpoint = isFollowing
      ? `${PUBLIC_API_URL}/api/followed-posts/unfollow/${id}`
      : `${PUBLIC_API_URL}/api/followed-posts/follow/${id}`;

    try {
      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...getAuthHeader(),
        },
      });

      if (!response.ok) throw new Error("Failed to follow/unfollow");

      // Toggle the states
      isFollowing = !isFollowing;
      followerCount += isFollowing ? 1 : -1;
    } catch (error) {
      console.error("Error during follow/unfollow:", error);
    }
  }

  // Handle tag hover
  async function handleTagHover(event, tag) {
    if (variant === "thumb") return; // Do nothing for thumb variant
    
    hoveredTag = tag;
    tooltipVisible = true;
    
    // Position the tooltip relative to the mouse pointer
    const rect = event.target.getBoundingClientRect();
    tooltipPosition = {
      x: rect.left + window.scrollX,
      y: rect.bottom + window.scrollY + 5 // 5px offset from the bottom of the tag
    };
    
    // Fetch image if needed
    tagImage = await fetchTagImage(tag);
  }
  
  // Hide tooltip when mouse leaves
  function handleTagLeave() {
    tooltipVisible = false;
    tagImage = null;
  }
</script>

<!-- Resolution Modal -->
{#if showResolutionModal}
  <div class="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
    <div class="bg-white dark:bg-neutral-950 rounded-md shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto border border-neutral-200 dark:border-neutral-800">
      <div class="p-6">
        <h2 class="text-xl font-semibold mb-4">Resolve Mystery Object</h2>
        
        {#if resolutionError}
          <div class="bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300 p-3 rounded-md mb-4">
            {resolutionError}
          </div>
        {/if}
        
        <div class="mb-4">
          <label for="resolution-description" class="block text-sm font-medium mb-1.5">Resolution Description*</label>
          <Textarea
            id="resolution-description"
            bind:value={resolutionDescription}
            class="w-full p-2 border rounded-lg text-sm bg-white dark:bg-neutral-800 border-neutral-200 dark:border-neutral-700"
            placeholder="Explain how this mystery object was resolved..."
            rows="4"
          />
        </div>
        
        <div class="mb-4">
          <h3 class="text-sm font-medium mb-2">Contributing Comments</h3>
          <p class="text-xs text-neutral-600 dark:text-neutral-400 mb-2">
            Select comments that contributed to the resolution:
          </p>
          
          <div id="comment-selection-placeholder" class="text-sm text-neutral-500 dark:text-neutral-400 italic">
            Comment selection will be handled by the thread page component
          </div>
        </div>
        
        <div class="flex justify-end gap-2 mt-6">
          <Button 
            variant="outline" 
            on:click={() => showResolutionModal = false}
            class="text-sm"
          >
            Cancel
          </Button>
          <Button 
            variant="default" 
            on:click={resolvePost}
            class="text-sm bg-teal-600 hover:bg-teal-700"
          >
            Resolve
          </Button>
        </div>
      </div>
    </div>
  </div>
{/if}

{#if variant === "thumb"}
  <!-- Modified thumb variant with added shine+scale effect on hover -->
  <div class="thumb-card flex flex-col bg-white dark:bg-neutral-950 shadow-sm border border-neutral-200 dark:border-neutral-800 rounded-md overflow-hidden h-full transition-all duration-300 hover:shadow-md relative">
    <!-- Steam-like glossy overlay effect -->
    <div class="gloss-effect absolute inset-0 z-0 pointer-events-none"></div>
    
    <!-- Image at top without padding -->
    {#if imageSrc}
      <div class="w-full aspect-[4/3] overflow-hidden">
        <img src={imageSrc} alt={title} class="w-full h-full object-cover" />
      </div>
    {:else if mediaFiles.length > 0}
      <div class="w-full aspect-[4/3] overflow-hidden">
        <img src={mediaFiles[0].url} alt={title} class="w-full h-full object-cover" on:error={handleImageError} />
      </div>
    {:else}
      <div class="w-full aspect-[4/3] bg-neutral-100 dark:bg-neutral-800 flex items-center justify-center">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-neutral-300 dark:text-neutral-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
      </div>
    {/if}
    
    <!-- Content below image -->
    <div class="p-4 flex flex-col flex-grow">
      <h3 class="font-medium text-neutral-900 dark:text-white text-lg mb-1 line-clamp-1">{title}</h3>
      
      <!-- Description - reduced to 1 line -->
      <p class="text-sm text-neutral-600 dark:text-neutral-400 mb-2 line-clamp-1">{description}</p>
      
      <!-- Tags -->
      {#if $tagDetails && $tagDetails.length > 0}
        <div class="flex flex-wrap gap-1.5 mb-3">
          {#each $tagDetails as tag}
            <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-neutral-100 dark:bg-neutral-800 text-neutral-800 dark:text-neutral-300 border border-neutral-200 dark:border-neutral-700">
              {tag.label}
            </span>
          {/each}
        </div>
      {/if}
      
      <!-- Footer with username and status -->
      <div class="mt-auto pt-3 border-t border-neutral-100 dark:border-neutral-800 flex justify-between items-center">
        <div class="text-xs text-neutral-500 dark:text-neutral-400">
          <span class="font-medium">{postedBy}</span>
          <!-- Replace text status with badge, matching thread variant -->
          {#if solved}
            <span class="inline-flex items-center ml-1.5 px-2 py-0.5 rounded-full text-[10px] font-medium bg-teal-100 dark:bg-teal-900/30 text-teal-800 dark:text-teal-300 border border-teal-200 dark:border-teal-800/50">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-2.5 w-2.5 mr-0.5" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
              </svg>
              Resolved
            </span>
          {:else}
            <span class="inline-flex items-center ml-1.5 px-2 py-0.5 rounded-full text-[10px] font-medium bg-rose-100 dark:bg-rose-900/30 text-rose-800 dark:text-rose-300 border border-rose-200 dark:border-rose-800/50">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-2.5 w-2.5 mr-0.5" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd"/>
              </svg>
              Unresolved
            </span>
          {/if}
        </div>
        
        <!-- Stats -->
        <div class="flex items-center gap-2 text-xs text-neutral-500 dark:text-neutral-400">
          <!-- Votes -->
          <div class="flex items-center gap-1">
            {#if downvotes > upvotes}
              <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 text-rose-600" viewBox="0 0 20 20" fill="currentColor">
                <path d="M5 6l5 5 5-5H5z"/>
              </svg>
              <span class="text-rose-600">{upvotes - downvotes}</span>
            {:else if upvotes > 0}
              <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 text-teal-600" viewBox="0 0 20 20" fill="currentColor">
                <path d="M5 14l5-5 5 5H5z"/>
              </svg>
              <span class="text-teal-600">{upvotes - downvotes}</span>
            {:else}
              <span class="text-neutral-500">0</span>
            {/if}
          </div>

          <!-- Comment count -->
          <div class="flex items-center gap-1">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            <span>{commentCount}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
{:else}
  <Card.Root
    class={`shadow-md hover:shadow-lg transition-all duration-300 ease-[cubic-bezier(0.4,0,0.2,1)]
  ${variant === "thumb" ? "bg-opacity-90 hover:bg-opacity-100 w-70 h-70 lg:hover:scale-[1.02] hover:-translate-y-1 rounded-md" : "bg-opacity-90 hover:bg-opacity-100 rounded-md"}`}
  >
    {#if variant === "thread"}
      <!-- Thread variant layout - voting column with content -->
      <div class="flex">
        <!-- Left column with voting - vertically centered -->
        <div
          class="flex flex-col items-center justify-center px-4 py-4 border-r border-neutral-100 dark:border-neutral-800"
        >
          <button
            class="flex items-center justify-center hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-full w-10 h-10 transition-colors
                 {userUpvoted ? 'text-teal-600' : 'text-neutral-600'}"
            on:click={() => handleVote(true)}
          >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" viewBox="0 0 20 20" fill="currentColor">
            <path d="M5 14l5-5 5 5H5z"/>
            </svg>
          </button>
          <span class="font-medium text-sm my-1.5">
            {#if downvotes > upvotes}
              <span class="text-rose-600">
                {upvotes - downvotes}
              </span>
            {:else if upvotes > 0}
              <span class="text-teal-600">
                {upvotes - downvotes}
              </span>
            {:else}
            <span class="text-neutral-500">
              0
            </span>
            {/if}
          </span>
          <button
            class="flex items-center justify-center hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-full w-10 h-10 transition-colors
                 {userDownvoted ? 'text-rose-600' : 'text-neutral-500'}"
            on:click={() => handleVote(false)}
          >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" viewBox="0 0 20 20" fill="currentColor">
            <path d="M5 6l5 5 5-5H5z"/>
            </svg>
          </button>
        </div>

        <!-- Right column with content - consistent padding with comment component -->
        <div class="flex-1 p-4">
          <!-- Post metadata -->
          <div class="flex flex-row justify-between">
            <div>
              <a
                href={`/user/${postedBy}`}
                class="font-medium hover:text-rose-900 hover:underline"
              >
                {postedBy || "Anonymous"}
              </a>
              <span>•</span>
              <span title={createdAt} class="text-neutral-500 dark:text-neutral-400">
                {formatDate(createdAt)}
              </span>
              {#if updatedAt && updatedAt !== createdAt}
                <span>•</span>
                <span title={updatedAt} class="text-neutral-500 dark:text-neutral-400">
                  edited {formatDate(updatedAt)}
                </span>
              {/if}
              {#if solved}
                <span>•</span>
                <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-teal-100 dark:bg-teal-900/30 text-teal-800 dark:text-teal-300 border border-teal-200 dark:border-teal-800/50">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mx-1.5 -ml-0.5" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>    
                  </svg>
                  Resolved
                </span>
              {:else}
                <span>•</span>
                <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-rose-100 dark:bg-rose-900/30 text-rose-800 dark:text-rose-300 border border-rose-200 dark:border-rose-800/50">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mx-1.5 -ml-0.5" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd"/>    
                  </svg>
                  Unresolved
                </span>
              {/if}
            </div>

            {#if $activeUser != null}
            <div>
              <button
              class={`text-xs px-4 py-1 rounded-full border-primary text-primary 
                hover:bg-[#0F766E] border-2 hover:border-[#0F993C] hover:text-white transition
         ${isFollowing ? 'bg-transparent' : 'bg-[#0F766E]'}`}

                on:click={toggleFollow}
              >
                {isFollowing ? "Following" : "Follow Post"}
              </button>
            </div>

            {/if}

          </div>

          <!-- Title and description -->
          <h2 class="text-xl font-semibold mb-2 text-neutral-900 dark:text-white">{title}</h2>

        {#if description}
          <div class="text-neutral-800 dark:text-neutral-200 mb-1 leading-relaxed whitespace-pre-line">
              {description}
            </div>
          {/if}

          <!-- Tags -->
          {#if $tagDetails.length > 0}
            <div class="flex flex-wrap gap-2 mt-2">
              {#each $tagDetails as tag}
                <span
                  class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-neutral-100 dark:bg-neutral-800 text-neutral-800 dark:text-neutral-300 hover:bg-neutral-200 dark:hover:bg-neutral-700 transition-colors border border-neutral-200 dark:border-neutral-700 cursor-pointer"
                  on:mouseenter={(e) => handleTagHover(e, tag)}
                  on:mouseleave={handleTagLeave}
                >
                  {tag.label}
                </span>
              {/each}
            </div>
          {/if}

          <div
          class="flex justify-start gap-6 text-sm text-neutral-600 dark:text-neutral-400 mt-2 mb-1"
        >
          <div>
            <span class="font-semibold">{followerCount}</span> Followers
          </div>
        </div>

        </div>
      </div>

      <!-- Separate section for action buttons - consistent styling with comment actions -->
      {#if currentUser === postedBy}
      <div class="px-4 py-2.5 border-t border-neutral-100 dark:border-neutral-800">
          <div class="flex flex-wrap items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              class="text-xs py-1 px-3 hover:bg-neutral-100 dark:hover:bg-neutral-800 border-neutral-300 dark:border-neutral-700 rounded-full"
              on:click={() => dispatch('edit')}
              >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-1" viewBox="0 0 20 20" fill="currentColor">
                <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />  
              </svg>
              Edit Post
            </Button>

            {#if !solved}
              <Button
                variant="outline"
                size="sm"
                class="text-xs py-1 px-3 border-teal-400 dark:border-teal-600 text-teal-700 dark:text-teal-400 hover:bg-teal-50 dark:hover:bg-teal-900/20 rounded-full"
                on:click={() => dispatch('resolve')}
                >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-1" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>  
                </svg>
                Mark as Resolved
              </Button>
            {:else}
              <Button
                variant="outline"
                size="sm"
                class="text-xs py-1 px-3 border-rose-400 dark:border-rose-600 text-rose-700 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-900/20 rounded-full"
                on:click={() => dispatch('unresolve')}
                >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-1" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>  
                </svg>
                Revert Resolution
              </Button>
            {/if}
          </div>
        </div>
      {/if}

      <!-- Resolution section if resolved - proper alignment -->
      {#if solved && resolution}
        <div class="px-4 pb-5 pt-0">
          <div class="bg-teal-50 dark:bg-teal-900/10 border border-teal-200 dark:border-teal-800/50 rounded-md overflow-hidden">
            <div class="px-5 py-4 flex items-start gap-3">
              <div class="mt-1 flex-shrink-0">
                <div class="w-10 h-10 rounded-full bg-teal-100 dark:bg-teal-800/30 flex items-center justify-center text-teal-600 dark:text-teal-400">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>  
                  </svg>
                </div>
              </div>
              <div class="flex-1">
                <h3 class="font-medium text-md text-teal-800 dark:text-teal-300 mb-2">Mystery Object Resolved</h3>
              <div class="text-neutral text-sm 800 dark:text-neutral-200 mb-3 whitespace-pre-line">
                  {resolution.description}
                </div>

                {#if resolution.resolvedAt}
                <p class="text-xs text-neutral-600 dark:text-neutral-400 mb-2">
                    Resolved on {formatDate(resolution.resolvedAt)}
                  </p>
                {/if}

                {#if resolution.contributingComments && resolution.contributingComments.length > 0}
                  <div class="mt-3">
                    <h4 class="text-sm font-medium mb-2 text-teal-700 dark:text-teal-400 flex items-center">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M18 5v8a2 2 0 01-2 2h-5l-5 4v-4H4a2 2 0 01-2-2V5a2 2 0 012-2h12a2 2 0 012 2zM7 8H5v2h2V8zm2 0h2v2H9V8zm6 0h-2v2h2V8z" clip-rule="evenodd" />  
                      </svg>
                      Contributing Comments
                    </h4>

                    <!-- Display contributing comment objects if available -->
                    {#if contributingCommentObjects && contributingCommentObjects.length > 0}
                    <div class="space-y-2 mt-2 max-h-60 overflow-y-auto border border-teal-200 dark:border-teal-800/50 rounded-md p-2 bg-white/50 dark:bg-neutral-950/50">
                      {#each contributingCommentObjects as commentObj (commentObj.id)}
                        <div class="p-2 rounded-md bg-white dark:bg-neutral-950 shadow-sm border border-neutral-100 dark:border-neutral-800 hover:border-teal-300 dark:hover:border-teal-700 transition-colors">
                          <div class="text-xs font-medium mb-1 flex items-center justify-between">
                            <span class="text-neutral-700 dark:text-neutral-300">
                              {commentObj.author}
                              {#if commentObj.commentType}
                                <span class="ml-1.5 px-1.5 py-0.5 rounded-full text-[10px] bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-400">
                                    {commentObj.commentType.toLowerCase()}
                                  </span>
                                {/if}
                              </span>
                              <span class="text-neutral-500 text-[10px]">{formatDate(commentObj.createdAt)}</span>
                          </div>
                          <p class="text-xs text-neutral-800 dark:text-neutral-200 line-clamp-2">{commentObj.content}</p>
                          </div>
                        {/each}
                      </div>
                    {:else}
                    <p class="text-xs text-neutral-600 dark:text-neutral-400 italic">
                      {resolution.contributingComments.length} contributing comment{resolution.contributingComments.length !== 1 ? 's' : ''} selected
                      </p>
                    {/if}
                  </div>
                {/if}
              </div>
            </div>
          </div>
        </div>
      {/if}

      <!-- Separator between main content and details -->
      <div class="h-px bg-neutral-200 dark:bg-neutral-800 mx-4"></div>

      <!-- Mystery Object and Media Carousel section - consistent padding -->
      <div class="flex flex-col lg:flex-row gap-6 p-4">
        <!-- Mystery Object Details -->
        {#if mysteryObject}
          <div class="lg:w-1/2 flex-grow order-2 lg:order-1">
            <div class="p-5 rounded-md bg-neutral-50 dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-800 shadow-sm">
              <h3 class="text-md font-medium mb-4 text-neutral-950 dark:text-white">Mystery Object Properties</h3>  
              <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                {#each Object.entries(mysteryObject).filter(([key]) => 
                  !skipProperties.includes(key) && 
                  !key.toLowerCase().includes('colorname') && 
                  key !== 'color_name' &&
                  key !== 'color_label'
                ) as [key, value]}
                  {#if value}
                  <div class="bg-white dark:bg-neutral-950 p-3 rounded-md border border-neutral-100 dark:border-neutral-700 hover:border-neutral-300 dark:hover:border-neutral-600 transition-colors shadow-sm">
                    <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1 object-part-name">
                      {key.split(/(?=[A-Z])/).join(' ').replace('_', ' ')}
                      </span>
                      <span class="text-neutral-900 dark:text-neutral-100">
                        {#if key === 'value'}
                        ${value}
                      {:else if ['handmade', 'oneOfAKind'].includes(key)}
                        Yes
                      {:else if key.startsWith('size')}
                        {value} cm
                      {:else if key === 'weight'}
                        {value}g
                      {:else if key === 'color' && isHexColor(value)}
                        <div class="flex items-center gap-2">
                          <div class="w-4 h-4 rounded border" style="background-color: {value};"></div>
                            <span class="text-sm">
                              {#if isLoadingColorNames.has(value)}
                                Loading...
                              {:else}
                                {#await fetchColorName(value)}
                                  Loading...
                                {:then colorName}
                                  {colorName}
                                  <span class="text-xs opacity-75 ml-1">({value})</span>
                                {:catch}
                                  {value}
                                {/await}
                              {/if}
                            </span>
                          </div>
                        {:else}
                          {value}
                        {/if}
                      </span>
                    </div>
                  {/if}
                {/each}
              </div>

              <!-- Show Sub-Parts Section if there are any parts to show -->
              {#if mysteryObjectSubParts && mysteryObjectSubParts.length > 0}
                <div class="mt-6">
                  <h4 class="font-medium text-base mb-3 text-neutral-800 dark:text-neutral-300 border-b border-neutral-200 dark:border-neutral-700 pb-2">Object Parts</h4>
                  <div class="space-y-4">
                    {#each mysteryObjectSubParts as part (part.id)}
                    <div class="bg-white dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-700 rounded-lg p-3 shadow-sm hover:shadow-md transition-shadow duration-200">
                      <div class="flex justify-between items-center mb-3 border-b border-neutral-100 dark:border-neutral-800 pb-2">
                        <h5 class="font-medium text-sm text-neutral-800 dark:text-neutral-200">{part.description || 'Unnamed Part'}</h5>
                        </div>

                        <!-- Attribute summary -->
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-2">
                          {#if part.material}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">MATERIAL</span>
                            <span class="text-neutral-900 dark:text-neutral-100">{part.material}</span>
                            </div>
                          {/if}
                          {#if part.color && !Object.keys(part).some(k => k.toLowerCase().includes('colorname') && k !== 'color')}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">COLOR</span>
                            <span class="flex items-center gap-2">
                              {#if isHexColor(part.color)}
                                <div class="w-4 h-4 rounded border" style="background-color: {part.color};"></div>
                                  <span class="text-sm">
                                    {#if isLoadingColorNames.has(part.color)}
                                      Loading...
                                    {:else}
                                      {#await fetchColorName(part.color)}
                                        Loading...
                                      {:then colorName}
                                        {colorName}
                                        <span class="text-xs opacity-75 ml-1">({part.color})</span>
                                      {:catch}
                                        {part.color}
                                      {/await}
                                    {/if}
                                  </span>
                                {:else}
                                  {#await fetchColorHexFromName(part.color)}
                                  <div class="w-4 h-4 rounded border bg-neutral-200"></div>
                                  {:then hexColor}
                                    <div class="w-4 h-4 rounded border" style="background-color: {hexColor || formatColorForDisplay(part.color)};"></div>
                                    <span class="text-sm">{part.color}</span>
                                  {:catch}
                                    <div class="w-4 h-4 rounded border" style="background-color: {formatColorForDisplay(part.color)};"></div>  
                                    <span class="text-sm">{part.color}</span>
                                  {/await}
                                {/if}
                              </span>
                            </div>
                          {/if}
                          {#if part.shape}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">SHAPE</span>
                            <span class="text-neutral-900 dark:text-neutral-100">{part.shape}</span>
                            </div>
                          {/if}
                          {#if part.texture}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">TEXTURE</span>
                            <span class="text-neutral-900 dark:text-neutral-100">{part.texture}</span>
                            </div>
                          {/if}
                          {#if part.writtenText}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">WRITTEN TEXT</span>
                            <span class="text-neutral-900 dark:text-neutral-100">{part.writtenText}</span>
                            </div>
                          {/if}
                          {#if part.hardness}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">HARDNESS</span>
                            <span class="text-neutral-900 dark:text-neutral-100">{part.hardness}</span>
                            </div>
                          {/if}
                          {#if part.timePeriod}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">TIME PERIOD</span>
                            <span class="text-neutral-900 dark:text-neutral-100">{part.timePeriod}</span>
                            </div>
                          {/if}
                          {#if part.smell}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">SMELL</span>
                            <span class="text-neutral-900 dark:text-neutral-100">{part.smell}</span>
                            </div>
                          {/if}
                          {#if part.taste}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">TASTE</span>
                            <span class="text-neutral-900 dark:text-neutral-100">{part.taste}</span>
                            </div>
                          {/if}
                          {#if part.value}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">VALUE</span>
                            <span class="text-neutral-900 dark:text-neutral-100">${part.value}</span>
                            </div>
                          {/if}
                          {#if part.pattern}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">PATTERN</span>
                            <span class="text-neutral-900 dark:text-neutral-100">{part.pattern}</span>
                            </div>
                          {/if}
                          {#if part.brand}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">BRAND</span>
                            <span class="text-neutral-900 dark:text-neutral-100">{part.brand}</span>
                            </div>
                          {/if}
                          {#if part.print}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">PRINT</span>
                            <span class="text-neutral-900 dark:text-neutral-100">{part.print}</span>
                            </div>
                          {/if}
                          {#if part.handmade}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">HANDMADE</span>
                            <span class="text-neutral-900 dark:text-neutral-100">Yes</span>
                            </div>
                          {/if}
                          {#if part.oneOfAKind}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">ONE OF A KIND</span>
                            <span class="text-neutral-900 dark:text-neutral-100">Yes</span>
                            </div>
                          {/if}
                          {#if part.item_condition}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">CONDITION</span>
                            <span class="text-neutral-900 dark:text-neutral-100">{part.item_condition}</span>
                            </div>
                          {/if}
                          {#if part.sizeX || part.sizeY || part.sizeZ}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">DIMENSIONS</span>
                            <span class="text-neutral-900 dark:text-neutral-100">
                                {#if part.sizeX}{part.sizeX}{/if}
                                {#if part.sizeY}x{part.sizeY}{/if}
                                {#if part.sizeZ}x{part.sizeZ}{/if} cm
                              </span>
                            </div>
                          {/if}
                          {#if part.weight}
                          <div class="bg-neutral-50 dark:bg-neutral-950 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                            <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">WEIGHT</span>
                            <span class="text-neutral-900 dark:text-neutral-100">{part.weight}g</span>
                            </div>
                          {/if}
                        </div>
                      </div>
                    {/each}
                  </div>
                </div>
              {/if}
            </div>
          </div>
        {/if}

        <!-- Media Carousel Section -->
        {#if mediaFiles.length > 0}
          <div class="lg:w-1/2 flex-shrink-0 order-1 lg:order-2">
            <div class="relative rounded-md overflow-hidden bg-neutral-50 dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-800 shadow-sm">
              <!-- Carousel navigation buttons -->
              {#if mediaFiles.length > 1}
              <div class="absolute top-0 bottom-0 left-0 flex items-center z-10">
                  <button
                    on:click={prevMedia}
                    class="bg-black/40 hover:bg-black/60 backdrop-blur-sm text-white p-2 rounded-r-lg ml-2 focus:outline-none transform transition-all duration-200 hover:scale-110 hover:shadow-lg"
                    aria-label="Previous media"
                  >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                    </svg>
                  </button>
                </div>

                <div class="absolute top-0 bottom-0 right-0 flex items-center z-10">
                  <button
                    on:click={nextMedia}
                    class="bg-black/40 hover:bg-black/60 backdrop-blur-sm text-white p-2 rounded-l-lg mr-2 focus:outline-none transform transition-all duration-200 hover:scale-110 hover:shadow-lg"
                    aria-label="Next media"
                  >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                    </svg>
                  </button>
                </div>

                <!-- Media counter -->
                <div class="absolute bottom-4 left-0 right-0 flex justify-center z-10">
                  <div class="bg-black/60 backdrop-blur-sm text-white px-3 py-1 rounded-full text-xs shadow-sm">  
                    {currentMediaIndex + 1} / {mediaFiles.length}
                  </div>
                </div>
              {/if}

              <!-- Current media display -->
              <div class="carousel-container w-full">
                {#if mediaFiles[currentMediaIndex]}
                  {@const media = mediaFiles[currentMediaIndex]}
                  {#if media.type === 'image'}
                  <img
                      src={media.url}
                      alt={media.name || title}
                      class="w-full object-contain max-h-[500px]"
                      on:error={handleMediaError}
                    />
                    {:else if media.type === 'video'}
                    <!-- svelte-ignore a11y-media-has-caption -->
                    <video
                      src={media.url}
                      controls
                      class="w-full object-contain max-h-[500px]"
                    >
                      Your browser does not support the video tag.
                    </video>
                  {:else}
                  <div class="flex flex-col items-center justify-center p-10 h-[300px]">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-24 w-24 text-rose-500 dark:text-rose-400 mb-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2zM9 10l12-3" />
                      </svg>
                      <audio 
                      src={media.url} 
                      controls 
                      class="w-full max-w-md"
                    >
                        Your browser does not support the audio tag.
                      </audio>
                      <p class="mt-4 text-gray-700 dark:text-gray-300">{media.name}</p>
                    </div>
                  {/if}
                {/if}
              </div>

              <!-- Thumbnail navigation for multiple media -->
              {#if mediaFiles.length > 1}
              <div class="flex justify-center gap-2 px-4 py-3 bg-neutral-100 dark:bg-neutral-950 overflow-x-auto">
                  {#each mediaFiles as media, i}
                    <button
                    on:click={() => currentMediaIndex = i}
                      class="flex-shrink-0 w-14 h-14 rounded-md overflow-hidden focus:outline-none transition-all duration-200 ease-in-out
                        {i === currentMediaIndex ? 'ring-2 ring-teal-500 transform scale-105 shadow-md' : 'opacity-60 hover:opacity-100 hover:shadow-sm'}"
                    aria-label={`View media item ${i+1}`}
                  >
                    {#if media.type === 'image'}
                      <img src={media.url} alt="thumbnail" class="w-full h-full object-cover" />
                    {:else if media.type === 'video'}
                      <div class="w-full h-full bg-neutral-200 dark:bg-neutral-700 flex items-center justify-center">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                          </svg>
                        </div>
                        {:else if media.type === 'audio'}
                        <div class="w-full h-full bg-neutral-200 dark:bg-neutral-800 flex items-center justify-center">
                          <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2zM9 10l12-3" />  
                          </svg>
                        </div>
                      {:else}
                      <div class="w-full h-full bg-neutral-200 dark:bg-neutral-800 flex items-center justify-center">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 01-2-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                          </svg>
                        </div>
                      {/if}
                    </button>
                  {/each}
                </div>
              {/if}
            </div>
          </div>
        {/if}
      </div>
    {/if}

    <!-- Tag tooltip -->
    {#if tooltipVisible && hoveredTag}
    <div class="tag-tooltip" style="left: {tooltipPosition.x}px; top: {tooltipPosition.y}px;">
        <div class="tag-tooltip-content">
          <div class="tag-tooltip-header">
            <span class="font-medium">{hoveredTag.label}</span>
          </div>
          <div class="tag-tooltip-body">
            {#if tagImageLoading}
              <div class="tag-image-placeholder">
                <div class="inline-block h-8 w-8 border-4 border-neutral-200 dark:border-neutral-800 border-t-teal-600 dark:border-t-teal-500 rounded-full animate-spin"></div>
              </div>
            {:else if tagImage}
              <img src={tagImage} alt={hoveredTag.label} class="tag-image" />
            {/if}
            <p class="tag-description">{hoveredTag.description || 'No description available'}</p>
          </div>
        </div>
      </div>
    {/if}
  </Card.Root>
{/if}

<style>
  /* Modern transitions and animations */
  .carousel-container {
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }
  
  /* Thumb card hover effects */
  .thumb-card {
    transform: translateZ(0); /* Hardware acceleration */
    transition: all 0.3s ease;
  }
  
  .thumb-card:hover {
    transform: scale(1.02);
  }
  
  /* Diagonal glossy effect from top-left to bottom-right */
  .gloss-effect {
    background: linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.15) 0%,
      rgba(255, 255, 255, 0.05) 50%,
      rgba(255, 255, 255, 0) 100%
    );
    opacity: 0.7;
    border-radius: 4px;
  }
  
  /* Enhanced gloss effect on hover */
  .thumb-card:hover .gloss-effect {
    background: linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.25) 0%,
      rgba(255, 255, 255, 0.08) 50%,
      rgba(255, 255, 255, 0) 100%
    );
    opacity: 0.9;
    transition: opacity 0.3s ease, background 0.3s ease;
  }
  
  /* Dark mode adjustments for gloss effect */
  :global(.dark) .gloss-effect {
    background: linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.07) 0%,
      rgba(255, 255, 255, 0.02) 50%,
      rgba(255, 255, 255, 0) 100%
    );
  }
  
  :global(.dark) .thumb-card:hover .gloss-effect {
    background: linear-gradient(
      135deg,
      rgba(255, 255, 255, 0.12) 0%,
      rgba(255, 255, 255, 0.04) 50%,
      rgba(255, 255, 255, 0) 100%
    );
  }
  
  /* Fade-in animation for cards */
  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(8px); }
    to { opacity: 1; transform: translateY(0); }
  }
  
  .card-animate {
    animation: fadeIn 0.4s ease-out;
  }

  /* Ensure text in object parts is displayed in all uppercase */
  :global(.object-part-name) {
    text-transform: uppercase;
  }
  
  /* Tag tooltip styles */
  .tag-tooltip {
    position: absolute;
    z-index: 100;
    max-width: 300px;
    background-color: white;
    padding: 12px;
    border-radius: 0.5rem;
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
    overflow: hidden;
    border: 1px solid #e5e7eb;
    animation: fadeIn 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  }
  
  :global(.dark) .tag-tooltip {
    background-color: #171717;
    border-color: #262626;
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.3), 0 10px 10px -5px rgba(0, 0, 0, 0.2);
  }
  
  .tag-tooltip-content {
    display: flex;
    flex-direction: column;
  }
  
  .tag-tooltip-header {
    padding: 10px 12px;
    border-bottom: 1px solid #e5e7eb;
    font-size: 14px;
    color: #171717;
  }
  
  :global(.dark) .tag-tooltip-header {
    border-color: #262626;
    color: #e5e5e5;
  }
  
  .tag-tooltip-body {
    padding: 12px;
  }
  
  .tag-image {
    max-width: 100%;
    max-height: 150px;
    margin-bottom: 10px;
    border-radius: 0.375rem;
    object-fit: contain;
  }
  
  .tag-image-placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100px;
    background-color: #f5f5f5;
    border-radius: 0.375rem;
    margin-bottom: 10px;
  }
  
  :global(.dark) .tag-image-placeholder {
    background-color: #262626;
  }
  
  .tag-description {
    font-size: 13px;
    line-height: 1.5;
    color: #525252;
  }
  
  :global(.dark) .tag-description {
    color: #a3a3a3;
  }
</style>