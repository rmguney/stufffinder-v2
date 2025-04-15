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

  export let id = '';
  export let title = '';
  export let description = '';
  export let tags = [];
  export let imageSrc = ''; // Keep for backward compatibility
  export let mediaFiles = []; // New prop for multiple media files
  export let solved = false;
  export let resolution = null;
  export let variant = "thumb";
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

  let tagDetails = writable([]);
  let currentUser = null; 
  let currentMediaIndex = 0; // Track the current displayed media in carousel
  let isLoadingSubParts = false;
  let subPartsError = null;

  let colorNames = new Map(); // Cache for color names to avoid duplicate API calls
  let isLoadingColorNames = new Set(); // Track which colors are currently loading

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
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="white" class="size-10 bg-black">
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

// Function to fetch color name from TheColorAPI - similar to colorPicker component
async function fetchColorName(hexColor) {
  if (!isHexColor(hexColor)) return null;
  
  // Return from cache if already fetched
  if (colorNames.has(hexColor)) {
    return colorNames.get(hexColor);
  }
  
  isLoadingColorNames.add(hexColor);
  isLoadingColorNames = isLoadingColorNames; // Trigger reactivity
  
  // Remove the # character from hex colors
  const cleanHex = hexColor.replace('#', '');
  
  try {
    const response = await fetch(`https://www.thecolorapi.com/id?hex=${cleanHex}`);
    if (!response.ok) {
      throw new Error('Failed to fetch color name');
    }
    const data = await response.json();
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

  $: activeUser.subscribe((value) => {
    currentUser = value;
  });

  // Update mysteryObjectSubParts whenever mysteryObject changes
  $: if (mysteryObject && mysteryObject.subParts) {
    mysteryObjectSubParts = mysteryObject.subParts;
  }

  onMount(() => {
    fetchTagDetails();
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
</script>

<!-- Resolution Modal -->
{#if showResolutionModal}
  <div class="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
    <div class="bg-white dark:bg-neutral-900 rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
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

<Card.Root class={`shadow-md hover:shadow-xl transition-all duration-300 ease-[cubic-bezier(0.4,0,0.2,1)]
  ${variant === "thumb" ? 'bg-opacity-90 hover:bg-opacity-100 w-70 h-70 lg:hover:scale-[1.02] hover:-translate-y-1' : 'bg-opacity-90 hover:bg-opacity-100'}`}>
  {#if variant === "thread"}
    <!-- Thread variant layout -->
    <div class="p-4">
      <!-- Top section with voting and main content -->
      <div class="flex gap-4">
        <!-- Left sidebar with voting -->
        <div class="flex flex-col items-center gap-1">
          <button 
            class="flex items-center justify-center hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-full w-8 h-8 transition-colors
                   {userUpvoted ? 'text-teal-600' : 'text-neutral-600'}"
            on:click={() => handleVote(true)}
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
              <path d="M5 14l5-5 5 5H5z"/>
            </svg>
          </button>
          <span class="font-medium text-sm">
            {#if downvotes > upvotes}
              <span class="text-rose-600">
                {downvotes}
              </span>
            {:else if upvotes > 0}
              <span class="text-teal-600">
                {upvotes}
              </span>
            {:else}
              <span class="text-neutral-500">
                0
              </span>
            {/if}
          </span>
          <button 
            class="flex items-center justify-center hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-full w-8 h-8 transition-colors
                   {userDownvoted ? 'text-rose-600' : 'text-neutral-500'}"
            on:click={() => handleVote(false)}
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
              <path d="M5 6l5 5 5-5H5z"/>
            </svg>
          </button>
        </div>

        <!-- Main content -->
        <div class="flex-1">
          <!-- Post metadata -->
          <div class="flex flex-wrap items-center gap-2 text-sm text-neutral-600 mb-2">
            <a href={`/user/${postedBy}`} class="font-medium hover:text-rose-900 hover:underline">
              {postedBy || 'Anonymous'}
            </a>
            <span>•</span>
            <span>{formatDate(createdAt)}</span>
            {#if updatedAt && updatedAt !== createdAt}
              <span>•</span>
              <span>edited {formatDate(updatedAt)}</span>
            {/if}
            <span>•</span>
            <div class="flex items-center gap-1">
              {#if solved}
                <span class="text-teal-800 font-medium flex items-center gap-1">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
                  </svg>
                  Resolved
                </span>
              {:else}
                <span class="text-rose-900 font-medium flex items-center gap-1">
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
                  </svg>
                  Unresolved
                </span>
              {/if}
            </div>
          </div>

          <!-- Title and description -->
          <h2 class="text-xl font-semibold mb-3">{title}</h2>
          {#if description}
            <p class="text-neutral-800 dark:text-neutral-200 mb-4">{description}</p>
          {/if}

          <!-- Tags -->
          {#if $tagDetails.length > 0}
            <div class="flex flex-wrap gap-2 mb-4">
              {#each $tagDetails as tag}
                <a href={`https://www.wikidata.org/wiki/${tag.id}`} 
                   target="_blank" 
                   rel="noopener noreferrer"
                   class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-neutral-100 text-neutral-800 hover:bg-neutral-200">
                  {tag.label}
                </a>
              {/each}
            </div>
          {/if}
        </div>
      </div>

      <!-- Resolution section if resolved -->
      {#if solved && resolution}
        <div class="mt-2 mb-4 bg-teal-50 dark:bg-teal-900/20 border border-teal-200 dark:border-teal-800 rounded-lg p-4">
          <div class="flex items-start gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-teal-600 dark:text-teal-400 mt-0.5 flex-shrink-0" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
            </svg>
            <div class="flex-1">
              <h3 class="font-medium text-teal-800 dark:text-teal-300 mb-1">Resolution</h3>
              <p class="text-neutral-800 dark:text-neutral-200 mb-3">{resolution.description}</p>
              
              {#if resolution.resolvedAt}
                <p class="text-xs text-neutral-500 dark:text-neutral-400 mb-2">
                  Resolved on {formatDate(resolution.resolvedAt)}
                </p>
              {/if}
              
              {#if resolution.contributingComments && resolution.contributingComments.length > 0}
                <div class="mt-2">
                  <h4 class="text-sm font-medium text-teal-800 dark:text-teal-300 mb-1">Contributing Comments</h4>
                  <p class="text-xs text-neutral-600 dark:text-neutral-400">
                    {resolution.contributingComments.length} comment{resolution.contributingComments.length !== 1 ? 's' : ''} contributed to this resolution
                  </p>
                </div>
              {/if}
              
              {#if currentUser === postedBy}
                <div class="mt-3">
                  <Button 
                    variant="outline" 
                    size="sm"
                    on:click={unresolvePost}
                    class="text-xs py-1 px-2 border-rose-300 dark:border-rose-700 text-rose-700 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-900/20"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
                    </svg>
                    Revert Resolution
                  </Button>
                </div>
              {/if}
            </div>
          </div>
        </div>
      {/if}
      
      <!-- Resolution button for post owner if not resolved -->
<!--       {#if !solved && currentUser === postedBy}
        <div class="mt-2 mb-4">
          <Button 
            variant="outline" 
            on:click={openResolutionModal}
            class="text-sm border-teal-300 dark:border-teal-700 text-teal-700 dark:text-teal-400 hover:bg-teal-50 dark:hover:bg-teal-900/20"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
            </svg>
            Mark as Resolved
          </Button>
        </div>
      {/if} -->
      
      <!-- Separator between main content and details -->
      <Separator class="my-4" />

      <!-- Mystery Object and Media Carousel section -->
      <div class="flex flex-col lg:flex-row gap-6 mt-4">
          <!-- Mystery Object Details -->
          {#if mysteryObject}
            <div class="lg:w-1/2 flex-grow order-2 lg:order-1"> 
              <div class="p-4 rounded-lg bg-neutral-50 dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-800">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                  {#each Object.entries(mysteryObject).filter(([key]) => 
                    !skipProperties.includes(key) && 
                    !key.toLowerCase().includes('colorname') && 
                    key !== 'color_name' &&
                    key !== 'color_label'
                  ) as [key, value]}
                    {#if value}
                      <div class="bg-white dark:bg-neutral-950 p-3 rounded-md border border-neutral-100 dark:border-neutral-700 hover:border-neutral-300 dark:hover:border-neutral-600 transition-colors">
                        <span class="block text-xs font-medium text-black dark:text-white mb-1">
                          {key.split(/(?=[A-Z])/).join(' ').replace('_', ' ').toUpperCase()}
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
                    <h4 class="font-medium text-base mb-3 text-neutral-700 dark:text-neutral-300 px-1">Object Parts</h4>
                    <div class="space-y-4">
                      {#each mysteryObjectSubParts as part (part.id)}
                        <div class="bg-white dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-700 rounded-lg p-3 shadow-sm hover:shadow-md transition-shadow duration-200">
                          <div class="flex justify-between items-center mb-3 border-b border-neutral-100 dark:border-neutral-800 pb-2">
                            <h5 class="font-medium text-sm text-neutral-800 dark:text-neutral-200">{part.description || 'Unnamed Part'}</h5>
                          </div>
                          
                          <!-- Attribute summary -->
                          <div class="grid grid-cols-1 md:grid-cols-2 gap-2">
                            {#if part.material}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                                <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">MATERIAL</span>
                                <span class="text-neutral-900 dark:text-neutral-100">{part.material}</span>
                              </div>
                            {/if}
                            {#if part.color && !Object.keys(part).some(k => k.toLowerCase().includes('colorname') && k !== 'color')}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                                <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">COLOR</span>
                                <span class="flex items-center gap-1">
                                  <span class="w-4 h-4 inline-block rounded border border-neutral-400" style="background-color: {part.color};"></span>
                                  <span class="text-neutral-900 dark:text-neutral-100">
                                    {#if isHexColor(part.color) && !isLoadingColorNames.has(part.color)}
                                      {#await fetchColorName(part.color) then colorName}
                                        {colorName || part.color}
                                      {/await}
                                    {:else}
                                      {part.color}
                                    {/if}
                                  </span>
                                </span>
                              </div>
                            {/if}
                            {#if part.shape}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                                <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">SHAPE</span>
                                <span class="text-neutral-900 dark:text-neutral-100">{part.shape}</span>
                              </div>
                            {/if}
                            {#if part.texture}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                                <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">TEXTURE</span>
                                <span class="text-neutral-900 dark:text-neutral-100">{part.texture}</span>
                              </div>
                            {/if}
                            {#if part.writtenText}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                                <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">WRITTEN TEXT</span>
                                <span class="text-neutral-900 dark:text-neutral-100">{part.writtenText}</span>
                              </div>
                            {/if}
                            {#if part.hardness}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                                <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">HARDNESS</span>
                                <span class="text-neutral-900 dark:text-neutral-100">{part.hardness}</span>
                              </div>
                            {/if}
                            {#if part.timePeriod}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                                <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">TIME PERIOD</span>
                                <span class="text-neutral-900 dark:text-neutral-100">{part.timePeriod}</span>
                              </div>
                            {/if}
                            {#if part.smell}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                                <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">SMELL</span>
                                <span class="text-neutral-900 dark:text-neutral-100">{part.smell}</span>
                              </div>
                            {/if}
                            {#if part.taste}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                                <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">TASTE</span>
                                <span class="text-neutral-900 dark:text-neutral-100">{part.taste}</span>
                              </div>
                            {/if}
                            {#if part.value}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                                <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">VALUE</span>
                                <span class="text-neutral-900 dark:text-neutral-100">${part.value}</span>
                              </div>
                            {/if}
                            {#if part.pattern}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                                <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">PATTERN</span>
                                <span class="text-neutral-900 dark:text-neutral-100">{part.pattern}</span>
                              </div>
                            {/if}
                            {#if part.brand}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                                <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">BRAND</span>
                                <span class="text-neutral-900 dark:text-neutral-100">{part.brand}</span>
                              </div>
                            {/if}
                            {#if part.print}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                                <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">PRINT</span>
                                <span class="text-neutral-900 dark:text-neutral-100">{part.print}</span>
                              </div>
                            {/if}
                            {#if part.handmade}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                                <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">HANDMADE</span>
                                <span class="text-neutral-900 dark:text-neutral-100">Yes</span>
                              </div>
                            {/if}
                            {#if part.oneOfAKind}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                                <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">ONE OF A KIND</span>
                                <span class="text-neutral-900 dark:text-neutral-100">Yes</span>
                              </div>
                            {/if}
                            {#if part.item_condition}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                                <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">CONDITION</span>
                                <span class="text-neutral-900 dark:text-neutral-100">{part.item_condition}</span>
                              </div>
                            {/if}
                            {#if part.sizeX || part.sizeY || part.sizeZ}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
                                <span class="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">DIMENSIONS</span>
                                <span class="text-neutral-900 dark:text-neutral-100">
                                  {#if part.sizeX}{part.sizeX}{/if}
                                  {#if part.sizeY}x{part.sizeY}{/if}
                                  {#if part.sizeZ}x{part.sizeZ}{/if} cm
                                </span>
                              </div>
                            {/if}
                            {#if part.weight}
                              <div class="bg-neutral-50 dark:bg-neutral-900 p-2 rounded-md border border-neutral-100 dark:border-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-700 transition-colors">
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
            <div class="relative rounded-lg overflow-hidden bg-neutral-100 dark:bg-neutral-950">
              <!-- Carousel navigation buttons -->
              {#if mediaFiles.length > 1}
                <div class="absolute top-0 bottom-0 left-0 flex items-center z-10">
              <button 
                on:click={prevMedia}
                class="bg-black/40 hover:bg-black/60 backdrop-blur-sm text-white p-3 rounded-r-lg ml-2 focus:outline-none transform transition-all duration-200 hover:scale-110 hover:shadow-lg"
                aria-label="Previous media"
              >
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                    </svg>
                  </button>
                </div>
                
                <div class="absolute top-0 bottom-0 right-0 flex items-center z-10">
                  <button 
                    on:click={nextMedia}
                    class="bg-black/40 hover:bg-black/60 backdrop-blur-sm text-white p-3 rounded-l-lg mr-2 focus:outline-none transform transition-all duration-200 hover:scale-110 hover:shadow-lg"
                    aria-label="Next media"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7-7" />
                    </svg>
                  </button>
                </div>
                
                <!-- Media counter -->
                <div class="absolute bottom-4 left-0 right-0 flex justify-center z-10">
                <div class="bg-black/60 backdrop-blur-sm text-white px-3 py-1 rounded-full text-sm shadow-sm">
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
                      class="w-full object-contain max-h-[600px]" 
                      on:error={handleMediaError}
                    />
                  {:else if media.type === 'video'}
                    <!-- svelte-ignore a11y-media-has-caption -->
                    <video 
                      src={media.url} 
                      controls 
                      class="w-full object-contain max-h-[600px]"
                    >
                      Your browser does not support the video tag.
                    </video>
                  {:else if media.type === 'audio'}
                    <div class="flex flex-col items-center justify-center p-10 h-[300px]">
                      <svg xmlns="http://www.w3.org/2000/svg" class="h-24 w-24 text-rose-500 dark:text-rose-400 mb-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
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
                  {:else}
                    <div class="flex items-center justify-center p-10 h-[300px]">
                      <div class="text-center">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-24 w-24 mx-auto text-blue-500 dark:text-blue-400 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 01-2-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                        <p class="text-gray-700 dark:text-gray-300">{media.name}</p>
                      </div>
                    </div>
                  {/if}
                {/if}
              </div>
              
              <!-- Thumbnail navigation for multiple media -->
              {#if mediaFiles.length > 1}
                <div class="flex justify-center mt-4 gap-2 px-4 py-2 overflow-x-auto">
                  {#each mediaFiles as media, i}
                    <button 
                      on:click={() => currentMediaIndex = i}
                      class="flex-shrink-0 w-16 h-16 rounded-md overflow-hidden focus:outline-none transition-all duration-200 ease-in-out 
                             {i === currentMediaIndex ? 'ring-2 ring-neutral-500 ring-offset-2 transform scale-110 shadow-md' : 'opacity-60 hover:opacity-100 hover:shadow-sm'}"
                    >
                      {#if media.type === 'image'}
                        <img src={media.url} alt="thumbnail" class="w-full h-full object-cover" />
                      {:else if media.type === 'video'}
                        <div class="w-full h-full bg-neutral-200 dark:bg-neutral-800 flex items-center justify-center">
                          <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                          </svg>
                        </div>
                      {:else if media.type === 'audio'}
                        <div class="w-full h-full bg-neutral-200 dark:bg-neutral-800 flex items-center justify-center">
                          <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
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
    </div>
  {:else}
    <!-- Thumb variant - keep existing code -->
    <Card.Header>
      <div class="flex flex-row items-center">
        <div class="flex-1 min-w-0">
          <Card.Title>
            <div class={`${variant === "thumb" ? 'text-ellipsis overflow-hidden whitespace-nowrap w-full max-w-full' : ''}`}>
              {title}
            </div>
          </Card.Title>
          <Card.Description class={`${variant === "thumb" ? 'lg:translate-y-2 text-ellipsis overflow-hidden whitespace-nowrap' : 'my-2.5'}`}>
            <div>
              <div>
                {#if postedBy}
                  <a href={`/user/${postedBy}`} class="hover:text-rose-900 hover:underline font-bold">
                    {postedBy}
                  </a>
                {:else}
                  <span class="font-bold">Anonymous</span>
                {/if}
                <!-- Improved date display -->
                {#if createdAt && createdAt !== 'undefined' && createdAt !== 'null'}
                  <span class="ml-1">at {formatDate(createdAt)}</span>
                {/if}
              </div>
              <div class={`${variant === "thumb" ? 'mt-1' : 'mt-1.5'}`}>
                {#if solved}
                  <div class="flex items-center text-teal-800 font-semibold">
                    <span class="text-teal-800 font-bold">Resolved</span>
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="size-5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  </div>
                {:else}
                  <div class="flex items-center text-rose-900 font-semibold">
                    <span class="text-rose-900 font-bold">Unresolved</span>
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 24 24" stroke-width="2" stroke="currentColor" class="size-5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </div>
                {/if}
              </div>
            </div>
          </Card.Description>
        </div>
      </div>
    </Card.Header>

    <Card.Content>
      {#if variant !== "thumb"}
        <div class="-mt-5">
          <!-- Post metadata section -->
          <div class="flex justify-between items-center mb-4">
            <div class="flex items-center gap-4">
              <!-- Voting section -->
              <div class="flex items-center gap-2">
                <button 
                  class="flex items-center justify-center hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-full w-8 h-8 transition-colors
                         {userUpvoted ? 'text-teal-600' : 'text-neutral-600'}"
                  on:click={() => handleVote(true)}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M5 14l5-5 5 5H5z"/>
                  </svg>
                </button>
                <span class="font-medium text-sm">
                  {#if downvotes > upvotes}
                    <span class="text-rose-600">
                      {downvotes}
                    </span>
                  {:else if upvotes > 0}
                    <span class="text-teal-600">
                      {upvotes}
                    </span>
                  {:else}
                    <span class="text-neutral-500">
                      0
                    </span>
                  {/if}
                </span>
                <button 
                  class="flex items-center justify-center hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-full w-8 h-8 transition-colors
                         {userDownvoted ? 'text-rose-600' : 'text-neutral-500'}"
                  on:click={() => handleVote(false)}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M5 6l5 5 5-5H5z"/>
                  </svg>
                </button>
              </div>
            </div>
            
            <!-- Creation/Update time -->
            <div class="text-sm text-netural-500">
              {#if updatedAt && updatedAt !== createdAt}
                <span>Updated: {formatDate(updatedAt)}</span>
              {:else}
                <span>Created: {formatDate(createdAt)}</span>
              {/if}
            </div>
          </div>

          <!-- Add description section right after metadata -->
          <div class="mb-4">
            {#if description}
              <p class="text-lg mb-2">{description}</p>
            {/if}
          </div>

          <!-- Tags section -->
          <div>
            <ul>
              <span class="text-md font-semibold text-black dark:text-white">Tags:</span>
              {#each $tagDetails as tag}
                <li>
                  <a href={`https://www.wikidata.org/wiki/${tag.id}`} 
                     target="_blank" 
                     rel="noopener noreferrer"
                     class="hover:underline text-black dark:text-white hover:text-rose-700 dark:hover:text-rose-900 text-md">
                    {tag.label}: {tag.description}
                  </a>
                </li>
              {/each}
            </ul>
          </div>

          <!-- Mystery Object Details -->
          {#if mysteryObject}
            <Separator class="mt-4 mb-2"/>
            <div class="flex flex-col">
              <ul>
                {#if mysteryObject.writtenText}<li class="mt-2"><span class="font-semibold text-md">Written Text:</span> {mysteryObject.writtenText}</li>{/if}
                {#if mysteryObject.color}
                  <li class="mt-2">
                    <span class="font-semibold text-md">Color:</span>
                    <div class="inline-flex items-center gap-2 ml-1">
                      {#if isHexColor(mysteryObject.color)}
                        <span class="w-4 h-4 inline-block rounded border" style="background-color: {mysteryObject.color};"></span>
                        {#if isLoadingColorNames.has(mysteryObject.color)}
                          Loading...
                        {:else}
                          {#await fetchColorName(mysteryObject.color)}
                            Loading...
                          {:then colorName}
                            {colorName}
                            <span class="text-xs opacity-75 ml-1">({mysteryObject.color})</span>
                          {:catch}
                            {mysteryObject.color}
                          {/await}
                        {/if}
                      {:else}
                        {mysteryObject.color}
                      {/if}
                    </div>
                  </li>
                {/if}
                {#if mysteryObject.shape}<li class="mt-2"><span class="font-semibold text-md">Shape:</span> {mysteryObject.shape}</li>{/if}
                {#if mysteryObject.descriptionOfParts}<li class="mt-2"><span class="font-semibold text-md">Parts Description:</span> {mysteryObject.descriptionOfParts}</li>{/if}
                {#if mysteryObject.location}<li class="mt-2"><span class="font-semibold text-md">Location:</span> {mysteryObject.location}</li>{/if}
                {#if mysteryObject.hardness}<li class="mt-2"><span class="font-semibold text-md">Hardness:</span> {mysteryObject.hardness}</li>{/if}
                {#if mysteryObject.timePeriod}<li class="mt-2"><span class="font-semibold text-md">Time Period:</span> {mysteryObject.timePeriod}</li>{/if}
                {#if mysteryObject.smell}<li class="mt-2"><span class="font-semibold text-md">Smell:</span> {mysteryObject.smell}</li>{/if}
                {#if mysteryObject.taste}<li class="mt-2"><span class="font-semibold text-md">Taste:</span> {mysteryObject.taste}</li>{/if}
                {#if mysteryObject.texture}<li class="mt-2"><span class="font-semibold text-md">Texture:</span> {mysteryObject.texture}</li>{/if}
                {#if mysteryObject.value}<li class="mt-2"><span class="font-semibold text-md">Value:</span> ${mysteryObject.value}</li>{/if}
                {#if mysteryObject.originOfAcquisition}<li class="mt-2"><span class="font-semibold text-md">Origin:</span> {mysteryObject.originOfAcquisition}</li>{/if}
                {#if mysteryObject.pattern}<li class="mt-2"><span class="font-semibold text-md">Pattern:</span> {mysteryObject.pattern}</li>{/if}
                {#if mysteryObject.brand}<li class="mt-2"><span class="font-semibold text-md">Brand:</span> {mysteryObject.brand}</li>{/if}
                {#if mysteryObject.print}<li class="mt-2"><span class="font-semibold text-md">Print:</span> {mysteryObject.print}</li>{/if}
                {#if mysteryObject.imageLicenses}<li class="mt-2"><span class="font-semibold text-md">Image Licenses:</span> {mysteryObject.imageLicenses}</li>{/if}
                {#if mysteryObject.handmade}<li class="mt-2"><span class="font-semibold text-md">Handmade:</span> Yes</li>{/if}
                {#if mysteryObject.oneOfAKind}<li class="mt-2"><span class="font-semibold text-md">One of a Kind:</span> Yes</li>{/if}
                {#if mysteryObject.item_condition}<li class="mt-2"><span class="font-semibold text-md">Condition:</span> {mysteryObject.item_condition}</li>{/if}
                {#if mysteryObject.sizeX || mysteryObject.sizeY || mysteryObject.sizeZ}
                  <li class="mt-2"><span class="font-semibold text-md">Dimensions:</span> {mysteryObject.sizeX}x{mysteryObject.sizeY}x{mysteryObject.sizeZ} cm</li>
                {/if}
                {#if mysteryObject.weight}<li class="mt-2"><span class="font-semibold text-md">Weight:</span> {mysteryObject.weight}g</li>{/if}
              </ul>
            </div>
          {/if}
        </div>
      {:else}
        <div class={`${variant === "thumb" ? 'overflow-hidden flex justify-center items-center mt-2 rounded-md' : ''}`}>
          {#if thumbnailImage}
            {#if variant !== "thumb"}
              <a href={thumbnailImage} target="_blank" rel="noopener noreferrer">
                <img 
                  class="object-cover w-full pt-4" 
                  src={thumbnailImage} 
                  alt={title}
                  on:error={handleImageError}
                />
              </a>
            {:else}
              <img 
                class="object-cover w-full pt-4 h-48 hover:scale-[1.02] transition-all duration-300" 
                src={thumbnailImage} 
                alt={title}
                on:error={handleImageError}
              />
            {/if}
          {/if}
        </div>

      <!-- Add brief description for thumb variant -->
      {#if variant === "thumb" && description}
        <div class="pt-3">
          <p class="text-sm text-neutral-700 dark:text-neutral-300 line-clamp-2">
            {description}
          </p>
        </div>
      {/if}

        <!-- Add tags display for thumb variant after the image -->
        {#if variant === "thumb" && $tagDetails.length > 0}
          <div class="flex flex-wrap gap-1.5 mt-3 mb-1">
            {#each $tagDetails as tag}
              <a href={`https://www.wikidata.org/wiki/${tag.id}`} 
                 target="_blank" 
                 rel="noopener noreferrer"
                 class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-neutral-100 text-neutral-800 hover:bg-neutral-200 transition-colors duration-200">
                {tag.label}
              </a>
            {/each}
          </div>
        {/if}

        <!-- Add comment count and vote count display -->
        {#if variant === "thumb"}
          <div class="flex items-center gap-2 text-xs text-neutral-600 dark:text-neutral-400 pt-3 pb-1 border-t border-neutral-100 dark:border-neutral-800 mt-2 min-h-[24px] px-2">
            <!-- Vote count -->
            <div class="flex items-center gap-1.5 h-[24px]">
              {#if downvotes > upvotes}
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 text-rose-600" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M5 6l5 5 5-5H5z"/>
                </svg>
                <span class="text-rose-600">{downvotes} downvotes</span>
              {:else if upvotes > 0}
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 text-teal-600" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M5 14l5-5 5 5H5z"/>
                </svg>
                <span class="text-teal-600">{upvotes} upvotes</span>
              {:else}
                <div class="flex items-center gap-1">
                  <div class="flex flex-col -my-1 justify-center">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 text-neutral-500" viewBox="0 0 20 20" fill="currentColor">
                      <path d="M5 14l5-5 5 5H5z"/>
                    </svg>
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 text-neutral-500" viewBox="0 0 20 20" fill="currentColor">
                      <path d="M5 6l5 5 5-5H5z"/>
                    </svg>
                  </div>
                  <span class="text-neutral-500">0 votes</span>
                </div>
              {/if}
            </div>

            <!-- Comment count -->
            <div class="flex items-center gap-1 h-[24px]">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              <span>{commentCount} {commentCount === 1 ? 'comment' : 'comments'}</span>
            </div>
          </div>
        {/if}
      {/if}
    </Card.Content>
  {/if}
</Card.Root>

<style>
  /* Modern transitions and animations */
  .carousel-container {
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }
  
  /* Fade-in animation for cards */
  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(8px); }
    to { opacity: 1; transform: translateY(0); }
  }
  
  .card-animate {
    animation: fadeIn 0.4s ease-out;
  }
</style>
