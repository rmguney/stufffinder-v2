<script>
  import * as Card from "$lib/components/ui/card";
  import { onMount } from "svelte";
  import { writable } from "svelte/store";
  import { activeUser } from "../../userStore";
  import { Button } from "$lib/components/ui/button";
  import { Separator } from "$lib/components/ui/separator";
  import { PUBLIC_API_URL } from "$env/static/public";

  export let id = '';
  export let title = '';
  export let description = '';
  export let tags = [];
  export let imageSrc = ''; // Keep for backward compatibility
  export let mediaFiles = []; // New prop for multiple media files
  export let solved = false;
  export let variant = "thumb";
  export let mysteryObject = null;  // Changed from object with defaults to null
  export let postedBy = '';
  export let postedDate = '';
  export let upvotes = 0;
  export let downvotes = 0;
  export let userUpvoted = false;
  export let userDownvoted = false;
  export let createdAt = '';
  export let updatedAt = '';

  let tagDetails = writable([]);
  let currentUser = null; 
  let currentMediaIndex = 0; // Track the current displayed media in carousel

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
  
  const toggleResolved = async () => {
    if (currentUser !== postedBy) return;
    try {
        const response = await fetch(`${PUBLIC_API_URL}/api/posts/${id}/markSolved`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
            }
        });

        if (!response.ok) throw new Error('Failed to toggle resolved status');
        const data = await response.json();
        solved = data.solved;
    } catch (error) {
        console.error("Error toggling resolved status:", error);
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
  event.target.src = '/placeholder-image.png';
}

function handleMediaError(event) {
  event.target.src = '/placeholder-image.png';
}

  $: activeUser.subscribe((value) => {
    currentUser = value;
  });

  onMount(() => {
    fetchTagDetails(); 
  });

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

<Card.Root class={`shadow-md hover:shadow-xl transition duration-200
  ${variant === "thumb" ? 'bg-opacity-75 hover:bg-opacity-100 w-70 h-70 lg:hover:scale-110' : 'bg-opacity-90 hover:bg-opacity-100'}`}>
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
          <span class="font-medium text-sm">{upvotes - downvotes}</span>
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

      <!-- Separator between main content and details -->
      <Separator class="my-4" />

      <!-- Mystery Object and Media Carousel section -->
      <div class="flex flex-col lg:flex-row gap-6 mt-4">
        <!-- Mystery Object Details -->
        {#if mysteryObject}
          <div class="lg:w-1/2 flex-grow order-2 lg:order-1"> 
            <div class="p-4 rounded-lg bg-neutral-50 dark:bg-neutral-950 border border-neutral-200 dark:border-neutral-800">
              <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                {#each Object.entries(mysteryObject) as [key, value]}
                  {#if value && !['id', 'images', 'description'].includes(key)}
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
                        {:else}
                          {value}
                        {/if}
                      </span>
                    </div>
                  {/if}
                {/each}
              </div>
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
                    class="bg-black bg-opacity-40 hover:bg-opacity-60 text-white p-2 rounded-r-lg ml-2 focus:outline-none transform transition hover:scale-110"
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
                    class="bg-black bg-opacity-40 hover:bg-opacity-60 text-white p-2 rounded-l-lg mr-2 focus:outline-none transform transition hover:scale-110"
                    aria-label="Next media"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                    </svg>
                  </button>
                </div>
                
                <!-- Media counter -->
                <div class="absolute bottom-4 left-0 right-0 flex justify-center z-10">
                  <div class="bg-black bg-opacity-60 text-white px-3 py-1 rounded-full text-sm">
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
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                        <p class="text-gray-700 dark:text-gray-300">{media.name}</p>
                      </div>
                    </div>
                  {/if}
                {/if}
              </div>
              
              <!-- Thumbnail navigation for multiple media -->
              {#if mediaFiles.length > 1}
                <div class="flex justify-center mt-4 gap-2 px-4 pb-2 overflow-x-auto">
                  {#each mediaFiles as media, i}
                    <button 
                      on:click={() => currentMediaIndex = i}
                      class="flex-shrink-0 w-16 h-16 rounded-md overflow-hidden focus:outline-none transition-all {i === currentMediaIndex ? 'ring-2 ring-rose-500 transform scale-110' : 'opacity-60 hover:opacity-100'}"
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
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
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
            </a>
          {:else}
            <img 
              class="object-cover w-full h-44" 
              src={imageSrc} 
              alt={title}
              on:error={handleImageError}
            />
          {/if}
        {/if}
      </div>
    </Card.Content>
  {/if}
</Card.Root>
